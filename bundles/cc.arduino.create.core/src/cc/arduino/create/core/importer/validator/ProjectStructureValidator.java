/**
 * Copyright (C) 2019 TypeFox and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package cc.arduino.create.core.importer.validator;

import static cc.arduino.create.core.CoreActivator.PLUGIN_ID;
import static org.eclipse.core.runtime.IStatus.ERROR;
import static org.eclipse.core.runtime.Status.OK_STATUS;

import java.io.File;
import java.nio.file.Path;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

public class ProjectStructureValidator {

    public IStatus validate(Path path, IProgressMonitor monitor) {
        Assert.isNotNull(path, "path");
        SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
        try {
            subMonitor.beginTask("Validating project structure...", 1);

            // CMakeLists.txt
            File cmakeLists = path.resolve("CMakeLists.txt").toFile();
            if (!cmakeLists.exists() || !cmakeLists.isFile() || !cmakeLists.canRead()) {
                return error("Couldn't find '" + cmakeLists.getName() + "' file.");
            }
            subMonitor.worked(1);

            // The `sketch` folder and it's content
            File sketch = path.resolve("sketch").toFile();
            if (!sketch.exists() || !sketch.isDirectory() || !sketch.canRead()) {
                return error("Couldn't find the '" + sketch.getName() + "' folder.");
            }
            if (sketch.listFiles(name -> name.toString().endsWith(".ino.cpp")).length < 1) {
                return error("Couldn't find any sketch files in the 'sketch' folder.");
            }
            subMonitor.worked(1);

            // The `core` folder but no content
            File core = path.resolve("core").toFile();
            if (!core.exists() || !core.isDirectory() || !core.canRead()) {
                return error("Couldn't find the '" + core.getName() + "' folder.");
            }
            subMonitor.worked(1);

        } finally {
            if (monitor != null) {
                monitor.done();
            }
        }
        return OK_STATUS;
    }

    private IStatus error(String message) {
        return new Status(ERROR, PLUGIN_ID, message);
    }

}
