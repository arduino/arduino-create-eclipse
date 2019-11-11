/**
 * Copyright (C) 2019 TypeFox and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package cc.arduino.create.ui.importer.wizard;

import static cc.arduino.create.ui.ImageDescriptorCache.ImageRef.IMPORT_WIZARD_BANNER;
import static cc.arduino.create.ui.importer.wizard.ImportWizardModel.SourceType.DIR;
import static cc.arduino.create.ui.importer.wizard.ImportWizardModel.SourceType.ZIP;
import static org.eclipse.swt.SWT.BORDER;
import static org.eclipse.swt.SWT.CHECK;
import static org.eclipse.swt.SWT.PUSH;
import static org.eclipse.swt.SWT.RADIO;
import static org.eclipse.swt.SWT.SHADOW_IN;
import static org.eclipse.swt.SWT.SHEET;
import static org.eclipse.swt.SWT.TRAVERSE_RETURN;
import static org.eclipse.swt.events.FocusListener.focusLostAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static org.eclipse.swt.layout.GridData.FILL_BOTH;
import static org.eclipse.swt.layout.GridData.FILL_HORIZONTAL;
import static org.eclipse.swt.layout.GridData.GRAB_HORIZONTAL;
import static org.eclipse.swt.layout.GridData.GRAB_VERTICAL;
import static org.eclipse.swt.layout.GridData.HORIZONTAL_ALIGN_FILL;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.google.common.io.Files;

public class ImportWizardPage extends WizardPage {

    private static final int COMBO_HISTORY_LENGTH = 20;
    private static final String[] FILE_IMPORT_MASK = { "*.zip" };
    private static final String DESCRIPTION = "Select a directory or archive exported from Arduino Create";
    private static final String STORE_DIRECTORIES = ImportWizardPage.class.getName() + ".STORE_DIRECTORIES";
    private static final String STORE_ARCHIVES = ImportWizardPage.class.getName() + ".STORE_ARCHIVES";
    private static final String STORE_ARCHIVE_SELECTED = ImportWizardPage.class.getName() + ".STORE_ARCHIVE_SELECTED";

    private static String previouslyBrowsedDirectory = "";
    private static String previouslyBrowsedArchive = "";

    private final ImportWizardModel model;

    private Text projectNameText;
    private Button projectFromDirectoryRadio;
    private Button projectFromArchiveRadio;
    private Combo directoryPathCombo;
    private Combo archivePathCombo;
    private Button browseDirectoriesButton;
    private Button browseArchivesButton;

    protected ImportWizardPage(ImportWizardModel model) {
        super("Import", "Import from Arduino Create", IMPORT_WIZARD_BANNER.asImageDescriptor());
        setDescription(DESCRIPTION);
        this.model = model;
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite control = new Composite(parent, NONE);
        setControl(control);
        control.setLayout(new GridLayout());
        control.setLayoutData(new GridData(FILL_BOTH | GRAB_HORIZONTAL | GRAB_VERTICAL));

        createProjectsRootControl(control);
        createConfigurationControl(control);

        restoreWidgetState();
        Dialog.applyDialogFont(control);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && this.projectFromDirectoryRadio.getSelection()) {
            this.directoryPathCombo.setFocus();
        }
        if (visible && this.projectFromArchiveRadio.getSelection()) {
            this.archivePathCombo.setFocus();
        }
    }

    public void storeWidgetState() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            saveInHistory(settings, STORE_DIRECTORIES, directoryPathCombo.getText());
            saveInHistory(settings, STORE_ARCHIVES, archivePathCombo.getText());
            settings.put(STORE_ARCHIVE_SELECTED, projectFromArchiveRadio.getSelection());
            // TODO: store/restore the state of all the check boxes?
        }
    }

    private void createProjectsRootControl(Composite parent) {
        Group group = new Group(parent, SHADOW_IN);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        layout.makeColumnsEqualWidth = false;
        layout.marginWidth = 0;
        group.setLayout(layout);
        group.setLayoutData(new GridData(FILL_HORIZONTAL));

        new Label(group, NONE).setText("Project name:");
        projectNameText = new Text(group, BORDER);
        GridData projectNameData = new GridData(HORIZONTAL_ALIGN_FILL | GRAB_HORIZONTAL);
        projectNameData.horizontalSpan = 2;
        projectNameData.widthHint = new PixelConverter(projectNameText).convertWidthInCharsToPixels(25);
        projectNameText.setLayoutData(projectNameData);
        projectNameText.addModifyListener(e -> {
            model.projectName = projectNameText.getText().trim();
            validateModel();
        });

        projectFromDirectoryRadio = new Button(group, RADIO);
        projectFromDirectoryRadio.setText("Roo&t directory:");

        directoryPathCombo = new Combo(group, BORDER);
        GridData directoryPathData = new GridData(HORIZONTAL_ALIGN_FILL | GRAB_HORIZONTAL);
        directoryPathData.widthHint = new PixelConverter(directoryPathCombo).convertWidthInCharsToPixels(25);
        directoryPathCombo.setLayoutData(directoryPathData);
        browseDirectoriesButton = new Button(group, PUSH);
        browseDirectoriesButton.setText("B&rowse...");
        setButtonLayoutData(browseDirectoriesButton);

        projectFromArchiveRadio = new Button(group, RADIO);
        projectFromArchiveRadio.setText("&Archive file:");

        archivePathCombo = new Combo(group, BORDER);
        GridData archivePathData = new GridData(HORIZONTAL_ALIGN_FILL | GRAB_HORIZONTAL);
        archivePathData.widthHint = new PixelConverter(archivePathCombo).convertWidthInCharsToPixels(25);
        archivePathCombo.setLayoutData(archivePathData);
        browseArchivesButton = new Button(group, PUSH);
        browseArchivesButton.setText("B&rowse...");
        setButtonLayoutData(browseArchivesButton);

        projectFromDirectoryRadio.setSelection(true);
        archivePathCombo.setEnabled(false);
        browseArchivesButton.setEnabled(false);

        browseDirectoriesButton.addSelectionListener(onSelect(e -> handleDirectoryButtonPressed()));
        browseArchivesButton.addSelectionListener(onSelect(e -> handleArchiveButtonPressed()));

        directoryPathCombo.addTraverseListener(e -> {
            if (e.detail == TRAVERSE_RETURN) {
                e.doit = false;
                updateModel(directoryPathCombo.getText().trim());
            }
        });
        directoryPathCombo.addFocusListener(onFocusLost(e -> updateModel(directoryPathCombo.getText().trim())));
        directoryPathCombo.addSelectionListener(onSelect(e -> updateModel(directoryPathCombo.getText().trim())));

        archivePathCombo.addTraverseListener(e -> {
            if (e.detail == TRAVERSE_RETURN) {
                e.doit = false;
                updateModel(archivePathCombo.getText().trim());
            }
        });
        archivePathCombo.addFocusListener(onFocusLost(e -> updateModel(archivePathCombo.getText().trim())));
        archivePathCombo.addSelectionListener(onSelect(e -> updateModel(archivePathCombo.getText().trim())));

        projectFromDirectoryRadio.addSelectionListener(onSelect(e -> directoryRadioSelected()));
        projectFromArchiveRadio.addSelectionListener(onSelect(e -> archiveRadioSelected()));
    }

    private void createConfigurationControl(Composite parent) {
        Group group = new Group(parent, SHADOW_IN);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginWidth = 0;
        group.setLayout(layout);
        group.setLayoutData(new GridData(FILL_HORIZONTAL));

        Button buildAfterImportButton = new Button(group, CHECK);
        buildAfterImportButton.setText("Automatically start the buld after import");
        buildAfterImportButton.setSelection(model.buildAfterImport);
        buildAfterImportButton.addSelectionListener(onSelect(e -> {
            model.buildAfterImport = buildAfterImportButton.getSelection();
        }));
    }

    private void archiveRadioSelected() {
        if (projectFromArchiveRadio.getSelection()) {
            model.type = ZIP;
            directoryPathCombo.setEnabled(false);
            browseDirectoriesButton.setEnabled(false);
            archivePathCombo.setEnabled(true);
            browseArchivesButton.setEnabled(true);
            updateModel(archivePathCombo.getText());
            archivePathCombo.setFocus();
        }
    }

    private void directoryRadioSelected() {
        if (projectFromDirectoryRadio.getSelection()) {
            model.type = DIR;
            directoryPathCombo.setEnabled(true);
            browseDirectoriesButton.setEnabled(true);
            archivePathCombo.setEnabled(false);
            browseArchivesButton.setEnabled(false);
            updateModel(directoryPathCombo.getText());
            directoryPathCombo.setFocus();
        }
    }

    private void updateModel(final String path) {
        if (model.type == ZIP) {
            model.zipPath = path;
        } else {
            model.dirPath = path;
        }
        validateModel();
    }

    private void validateModel() {
        if (model.projectName == null && model.getPath() != null) {
            model.projectName = getCandidateProjectName();
            // XXX: hack
            projectNameText.setText(model.projectName);
            projectNameText.setSelection(projectNameText.getText().length());
        }

        setMessage(null);
        if (model.getPath() == null) {
            setPageComplete(false);
            setMessage(DESCRIPTION, IMessageProvider.NONE);
        } else {
            IStatus status = model.validate(null);
            setPageComplete(status.isOK());
            setMessage(status.isOK() ? null : status.getMessage(), toMessageProviderType(status));
        }
    }

    private String getCandidateProjectName() {
        String fileName = Files.getNameWithoutExtension(model.getPath().getFileName().toString());
        String candidateName = fileName;
        boolean valid = !ResourcesPlugin.getWorkspace().getRoot().getProject(candidateName).exists();
        int i = 2;
        while (!valid) {
            candidateName = fileName + "_" + i;
            valid = !ResourcesPlugin.getWorkspace().getRoot().getProject(candidateName).exists();
            i++;
        }
        return candidateName;
    }

    private void restoreWidgetState() {
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            restoreFromHistory(settings, STORE_DIRECTORIES, directoryPathCombo);
            restoreFromHistory(settings, STORE_ARCHIVES, archivePathCombo);
        }

        String initialPath = model.initialPath;
        if (initialPath == null && settings != null) {
            boolean archiveSelected = settings.getBoolean(STORE_ARCHIVE_SELECTED);
            projectFromDirectoryRadio.setSelection(!archiveSelected);
            projectFromArchiveRadio.setSelection(archiveSelected);
            if (archiveSelected) {
                archiveRadioSelected();
            } else {
                directoryRadioSelected();
            }
        } else if (initialPath != null) {
            boolean dir = new File(model.initialPath).isDirectory();
            projectFromDirectoryRadio.setSelection(dir);
            projectFromArchiveRadio.setSelection(!dir);
            if (dir) {
                directoryPathCombo.setText(initialPath);
                directoryPathCombo.setSelection(new Point(initialPath.length(), initialPath.length()));
                directoryRadioSelected();
            } else {
                archivePathCombo.setText(initialPath);
                archivePathCombo.setSelection(new Point(initialPath.length(), initialPath.length()));
                archiveRadioSelected();
            }
        }
    }

    private void restoreFromHistory(IDialogSettings settings, String key, Combo combo) {
        String[] sourceNames = settings.getArray(key);
        if (sourceNames == null) {
            return;
        }

        for (String sourceName : sourceNames) {
            combo.add(sourceName);
        }
    }

    private void handleDirectoryButtonPressed() {
        DirectoryDialog dialog = new DirectoryDialog(directoryPathCombo.getShell(), SHEET);
        dialog.setMessage("Select root directory of the projects to import");
        String dirName = directoryPathCombo.getText().trim();
        if (dirName.length() == 0) {
            dirName = previouslyBrowsedDirectory;
        }

        if (dirName.length() == 0) {
            dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
        } else {
            File path = new File(dirName);
            if (path.exists()) {
                dialog.setFilterPath(new Path(dirName).toOSString());
            }
        }

        String selectedDirectory = dialog.open();
        if (selectedDirectory != null) {
            previouslyBrowsedDirectory = selectedDirectory;
            directoryPathCombo.setText(previouslyBrowsedDirectory);
            updateModel(selectedDirectory);
        }
    }

    private void handleArchiveButtonPressed() {
        FileDialog dialog = new FileDialog(archivePathCombo.getShell(), SHEET);
        dialog.setFilterExtensions(FILE_IMPORT_MASK);
        dialog.setText("Select archive containing the projects to import");

        String fileName = archivePathCombo.getText().trim();
        if (fileName.length() == 0) {
            fileName = previouslyBrowsedArchive;
        }

        if (fileName.length() == 0) {
            dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
        } else {
            File path = new File(fileName).getParentFile();
            if (path != null && path.exists()) {
                dialog.setFilterPath(path.toString());
            }
        }

        String selectedArchive = dialog.open();
        if (selectedArchive != null) {
            previouslyBrowsedArchive = selectedArchive;
            archivePathCombo.setText(previouslyBrowsedArchive);
            updateModel(selectedArchive);
        }
    }

    private SelectionListener onSelect(Consumer<SelectionEvent> consumer) {
        return widgetSelectedAdapter(consumer);
    }

    private FocusListener onFocusLost(Consumer<FocusEvent> consumer) {
        return focusLostAdapter(consumer);
    }

    private int toMessageProviderType(/* nullable */IStatus status) {
        if (status == null) {
            return IMessageProvider.NONE;
        }
        switch (status.getSeverity()) {
        case IStatus.OK:
            return IMessageProvider.NONE;
        case IStatus.INFO:
            return IMessageProvider.INFORMATION;
        case IStatus.WARNING:
            return IMessageProvider.WARNING;
        case IStatus.ERROR:
            return IMessageProvider.ERROR;
        case IStatus.CANCEL:
            return IMessageProvider.NONE;
        default:
            throw new IllegalArgumentException("Unhandled status severity: " + status.getSeverity() + "\n" + status);
        }
    }

    /* default */ static void saveInHistory(IDialogSettings settings, String key, String value) {
        String[] sourceNames = settings.getArray(key);
        if (sourceNames == null) {
            sourceNames = new String[0];
        }
        sourceNames = addToHistory(sourceNames, value);
        settings.put(key, sourceNames);
    }

    /* default */ static String[] addToHistory(String[] history, String newEntry) {
        List<String> items = new ArrayList<>(Arrays.asList(history));
        addToHistory(items, newEntry);
        String[] r = new String[items.size()];
        items.toArray(r);
        return r;
    }

    /* default */ static void addToHistory(List<String> history, String newEntry) {
        history.remove(newEntry);
        history.add(0, newEntry);

        if (history.size() > COMBO_HISTORY_LENGTH) {
            history.remove(COMBO_HISTORY_LENGTH);
        }
    }

}
