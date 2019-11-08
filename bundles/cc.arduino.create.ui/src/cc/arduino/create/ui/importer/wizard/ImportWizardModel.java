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

import static cc.arduino.create.ui.UIActivator.error;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.google.common.base.Strings;

import cc.arduino.create.core.importer.validator.ProjectStructureValidator;

public class ImportWizardModel {

    public static enum SourceType {
        DIR, ZIP
    }

    public static String TYPE = "type";
    public static String DIR_PATH = "dirPath";
    public static String ZIP_PATH = "zipPath";

    public SourceType type = SourceType.DIR;
    public String initialPath; // UI/UX: Sugar for getting an initial path from a structured selection.
    public String projectName;
    public String dirPath;
    public String zipPath;
    public boolean buildAfterImport = true;

    public Path getPath() {
        String path = SourceType.ZIP == type ? zipPath : dirPath;
        return Strings.isNullOrEmpty(path) ? null : Paths.get(path);
    }

    public IStatus validate(IProgressMonitor monitor) {
        if (Strings.isNullOrEmpty(projectName)) {
            return error("The project name should be specified.");
        }
        IProject existingProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (existingProject != null && existingProject.exists()) {
            return error("The '" + projectName + "' already exists.");
        }
        return new ProjectStructureValidator().validate(getPath(), monitor);
    }

}
