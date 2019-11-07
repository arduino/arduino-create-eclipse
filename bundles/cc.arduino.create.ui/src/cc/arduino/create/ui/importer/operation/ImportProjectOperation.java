/**
 * Copyright (C) 2019 TypeFox and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package cc.arduino.create.ui.importer.operation;

import static cc.arduino.create.core.utils.ZipUtils.unzip;
import static cc.arduino.create.ui.UIActivator.error;
import static cc.arduino.create.ui.importer.wizard.ImportWizardModel.SourceType.ZIP;
import static java.nio.file.Files.createTempDirectory;
import static org.eclipse.core.resources.IncrementalProjectBuilder.CLEAN_BUILD;
import static org.eclipse.core.resources.IncrementalProjectBuilder.INCREMENTAL_BUILD;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.util.CDTListComparator;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICMultiConfigDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildProperty;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyManager;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyType;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;
import org.eclipse.cdt.managedbuilder.core.BuildListComparator;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.cdt.managedbuilder.internal.core.MultiConfiguration;
import org.eclipse.cdt.managedbuilder.internal.ui.commands.BuildConfigurationsJob;
import org.eclipse.cdt.managedbuilder.tcmodification.IConfigurationModification;
import org.eclipse.cdt.managedbuilder.tcmodification.IFolderInfoModification;
import org.eclipse.cdt.managedbuilder.tcmodification.IToolChainModificationManager;
import org.eclipse.cdt.managedbuilder.ui.wizards.CfgHolder;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSWizardHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

import cc.arduino.create.core.managedbuilders.ToolChainUtils;
import cc.arduino.create.core.managedbuilders.ToolChainUtils.Options;
import cc.arduino.create.ui.importer.wizard.ImportWizardModel;

@SuppressWarnings("restriction")
public class ImportProjectOperation extends WorkspaceModifyOperation {

    private static final String PROPERTY = "org.eclipse.cdt.build.core.buildType"; //$NON-NLS-1$
    private static final String PROP_VAL = PROPERTY + ".debug"; //$NON-NLS-1$
    // We rely on `cmake4eclipse`.
    private static final String CMAKE_BUILDER_PORTABLE_ID = "de.marw.cdt.cmake.core.genscriptbuilder";

    private final ImportWizardModel model;

    public ImportProjectOperation(ImportWizardModel model) {
        Assert.isNotNull(model, "model");
        this.model = model;
    }

    @Override
    protected void execute(IProgressMonitor monitor)
            throws CoreException, InvocationTargetException, InterruptedException {

        SubMonitor subMonitor = SubMonitor.convert(monitor, "Importing project into the workspace...", 8);

        // Validate
        subMonitor.subTask("Validating project structure...");
        IStatus status = model.validate(subMonitor.newChild(1));
        if (!status.isOK()) {
            throw new CoreException(status);
        }

        try {
            // Import
            subMonitor.subTask("Importing project into the workspace...");
            Path path = model.type == ZIP ? unzip(model.getPath(), createTempDirectory(null)) : model.getPath();
            File file = path.toFile();
            if (model.type == ZIP && file.isDirectory()) {
                File[] listFiles = file.listFiles();
                // XXX: this should not be here.
                if (listFiles != null && listFiles.length == 1 && "_cmake".equals(listFiles[0].getName())) {
                    file = listFiles[0];
                }
            }
            IPath containerPath = new org.eclipse.core.runtime.Path(model.projectName);
            IImportStructureProvider provider = FileSystemStructureProvider.INSTANCE;
            IOverwriteQuery overwriteQuery = ignored -> IOverwriteQuery.CANCEL;
            List<File> filesToImport = Arrays.asList(file.listFiles());
            ImportOperation operation = new ImportOperation(containerPath, provider, overwriteQuery, filesToImport);
            operation.setCreateContainerStructure(false);
            operation.run(subMonitor.newChild(1));

            // Configure project
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(model.projectName);
            if (project == null || !project.exists()) {
                throw new CoreException(error("The '" + model.projectName + "' project does not exist."));
            }

            if (!project.isOpen()) {
                project.open(subMonitor.newChild(1));
            }

            subMonitor.subTask("Initializing default C++ configuration...");
            createDefaultCProjectDescription(project, subMonitor.newChild(1));

            subMonitor.subTask("Registring CDT project...");
            CCorePlugin.getDefault().createCDTProject(project.getDescription(), project, subMonitor.newChild(1));
            subMonitor.subTask("Adding C nature...");
            CProjectNature.addCNature(project, subMonitor.newChild(1));
            subMonitor.subTask("Adding C++ nature...");
            CCProjectNature.addCCNature(project, subMonitor.newChild(1));

            // The followings must be configured:
            // 1. Builder Type: `External Builder`.
            // 2. Enable: `Use default build command`.
            // 3. Enable: `Makefile generation`> `Generate Makefiles automatically`.
            // 4. Current builder: `CMake Builder (portable)`.
            // Note: the 1-3 steps are the default ones. Nothing to do.

            // 4.
            ICProjectDescription description = CoreModel.getDefault().getProjectDescription(project);
            if (description == null) {
                throw new CoreException(error("Could not retrieve project descrition for '" + project + "'."));
            }
            ICConfigurationDescription[] configurationDescriptions = description.getConfigurations();
            if (configurationDescriptions == null || configurationDescriptions.length == 0) {
                throw new CoreException(error("No configuration descriptions were available for '" + project + "'."));
            }
            Arrays.sort(configurationDescriptions, CDTListComparator.getInstance());

            IToolChainModificationManager toolChainManager = ManagedBuildManager.getToolChainModificationManager();
            SubMonitor configMonitor = subMonitor.newChild(1);
            configMonitor.beginTask("Configuring builder for the project...", configurationDescriptions.length);
            subMonitor.subTask("Configuring builder for the project...");
            for (ICConfigurationDescription configurationDescription : configurationDescriptions) {
                // Project based setting (it is OK to get the root folder)
                ICFolderDescription rootFolderDescription = configurationDescription.getRootFolderDescription();
                IConfiguration configuration = getConfiguration(rootFolderDescription.getConfiguration());
                IResourceInfo resourceInfo = configuration.getResourceInfo(rootFolderDescription.getPath(), false);
                IFolderInfoModification createModification = toolChainManager
                        .createModification((IFolderInfo) resourceInfo);

                IBuilder realBuilder = ManagedBuildManager.getRealBuilder(configuration.getBuilder());
                if (realBuilder == null) {
                    realBuilder = configuration.getBuilder();
                }

                IBuilder[] compatibleBuilders = new IBuilder[0];
                List<IBuilder> allBuilders = new ArrayList<>();

                if (resourceInfo instanceof IFolderInfo) {
                    IFolderInfoModification folderInModification = createModification;
                    if (folderInModification instanceof IConfigurationModification) {
                        compatibleBuilders = ((IConfigurationModification) folderInModification)
                                .getCompatibleBuilders();
                        IBuilder[] copyCompatibleBuilders = new IBuilder[compatibleBuilders.length + 1];
                        System.arraycopy(compatibleBuilders, 0, copyCompatibleBuilders, 0, compatibleBuilders.length);
                        copyCompatibleBuilders[compatibleBuilders.length] = realBuilder;
                        compatibleBuilders = copyCompatibleBuilders;
                    }
                }
                for (int i = 0; i < compatibleBuilders.length; i++) {
                    if (compatibleBuilders[i].isSystemObject() && !compatibleBuilders[i].equals(realBuilder)) {
                        continue;
                    }
                    allBuilders.add(compatibleBuilders[i]);
                }
                Collections.sort(allBuilders, BuildListComparator.getInstance());
                Optional<IBuilder> findFirst = allBuilders.stream()
                        .filter(c -> c.getId().equals(CMAKE_BUILDER_PORTABLE_ID))
                        .findFirst();
                if (findFirst.isPresent()) {
                    ((IConfigurationModification) createModification).setBuilder(findFirst.get());
                } else {
                    throw new CoreException(
                            error("Could not find builder '" + CMAKE_BUILDER_PORTABLE_ID + "' on classpath."));
                }
                createModification.apply();
                toolChainManager.createModification(configuration, createModification).apply();
                CoreModel.getDefault().setProjectDescription(project, description);
                configMonitor.worked(1);
            }

            CCorePlugin.getIndexManager().reindex(CoreModel.getDefault().create(project));
            if (model.buildAfterImport) {
                scheduleBuild(project);
            }
        } catch (CoreException | InvocationTargetException | InterruptedException t) {
            tryClean();
            throw t;
        } catch (Exception e) {
            tryClean();
            throw new InvocationTargetException(e);
        }
    }

    // Extracted from: org.eclipse.cdt.managedbuilder.ui.wizards.ManagedBuildWizard.createItems(boolean, IWizard).
    // Also reused logic from:
    // org.eclipse.cdt.managedbuilder.ui.wizards.MBSWizardHandler.setProjectDescription(IProject, boolean, boolean,
    // IProgressMonitor).
    private void createDefaultCProjectDescription(IProject project, IProgressMonitor monitor)
            throws CoreException {
        ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
        ICProjectDescription des = mngr.createProjectDescription(project, false, false);
        ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
        monitor.worked(10);

        IBuildPropertyManager bpm = ManagedBuildManager.getBuildPropertyManager();
        IBuildPropertyType bpt = bpm.getPropertyType(MBSWizardHandler.ARTIFACT);
        IBuildPropertyValue[] vs = bpt.getSupportedValues();
        Arrays.sort(vs, BuildListComparator.getInstance());

        Map<String, IToolChain> items = new HashMap<>();
        // new style project types
        for (int i = 0; i < vs.length; i++) {
            IToolChain[] tcs = ManagedBuildManager.getExtensionsToolChains(MBSWizardHandler.ARTIFACT, vs[i].getId(),
                    false);
            if (tcs == null || tcs.length == 0) {
                continue;
            }
            IBuildPropertyValue propertyValue = vs[i];
            for (int j = 0; j < tcs.length; j++) {
                IToolChain tc = tcs[j];
                if (ToolChainUtils.isValid(tc, true, Options.DEFAULT)) {
                    IConfiguration[] cfgs = ManagedBuildManager.getExtensionConfigurations(tc,
                            MBSWizardHandler.ARTIFACT, propertyValue.getId());
                    if (cfgs != null && cfgs.length > 0) {
                        items.put(tc.getUniqueRealName(), tc);
                    }
                }
            }
        }

        if (items.isEmpty()) {
            throw new CoreException(error("Cannot create managed project with NULL configuration"));
        }

        CfgHolder[] cfgs = CfgHolder
                .cfgs2items(ManagedBuildManager.getExtensionConfigurations(items.values().iterator().next(),
                        MBSWizardHandler.ARTIFACT, "org.eclipse.cdt.build.core.buildArtefactType.exe"));

        if (cfgs == null || cfgs.length == 0 || cfgs[0].getConfiguration() == null) {
            throw new CoreException(error("Cannot create managed project with NULL configuration"));
        }

        Configuration cf = (Configuration) cfgs[0].getConfiguration();
        ManagedProject mProj = new ManagedProject(project, cf.getProjectType());
        info.setManagedProject(mProj);
        monitor.worked(10);
        cfgs = CfgHolder.unique(cfgs);
        cfgs = CfgHolder.reorder(cfgs);

        ICConfigurationDescription cfgDebug = null;
        ICConfigurationDescription cfgFirst = null;

        int work = 50 / cfgs.length;

        for (CfgHolder cfg : cfgs) {
            cf = (Configuration) cfg.getConfiguration();
            String id = ManagedBuildManager.calculateChildId(cf.getId(), null);
            Configuration config = new Configuration(mProj, cf, id, false, true);
            CConfigurationData data = config.getConfigurationData();
            ICConfigurationDescription cfgDes = des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
            config.setConfigurationDescription(cfgDes);
            config.exportArtifactInfo();

            IBuilder bld = config.getEditableBuilder();
            if (bld != null) {
                bld.setManagedBuildOn(true);
            }

            config.setName(cfg.getName());
            config.setArtifactName(mProj.getDefaultArtifactName());

            IBuildProperty b = config.getBuildProperties().getProperty(PROPERTY);
            if (cfgDebug == null && b != null && b.getValue() != null && PROP_VAL.equals(b.getValue().getId()))
                cfgDebug = cfgDes;
            if (cfgFirst == null) // select at least first configuration
                cfgFirst = cfgDes;
            monitor.worked(work);
        }
        mngr.setProjectDescription(project, des);
    }

    private void tryClean() throws CoreException {
        IProject toCleanUp = ResourcesPlugin.getWorkspace().getRoot().getProject(model.projectName);
        if (toCleanUp != null && toCleanUp.exists()) {
            toCleanUp.delete(true, null);
        }
    }

    private void scheduleBuild(IProject project) {
        ICProjectDescription projectDescription = CoreModel.getDefault().getProjectDescription(project, false);
        if (projectDescription != null) {
            ICConfigurationDescription activeConfiguration = projectDescription.getActiveConfiguration();
            if (activeConfiguration != null) {
                new BuildConfigurationsJob(new ICConfigurationDescription[] { activeConfiguration },
                        CLEAN_BUILD, INCREMENTAL_BUILD).schedule();
            }
        }
    }

    private IConfiguration getConfiguration(ICConfigurationDescription description) {
        return description instanceof ICMultiConfigDescription
                ? new MultiConfiguration(
                        (ICConfigurationDescription[]) ((ICMultiConfigDescription) description).getItems())
                : ManagedBuildManager.getConfigurationForDescription(description);
    }

}
