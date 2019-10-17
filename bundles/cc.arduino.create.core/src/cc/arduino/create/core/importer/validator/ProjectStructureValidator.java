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

import static cc.arduino.create.core.CoreActivator.error;
import static cc.arduino.create.core.ZipUtils.isZip;
import static cc.arduino.create.core.ZipUtils.unzip;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.list;
import static org.eclipse.core.runtime.Status.OK_STATUS;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;

import cc.arduino.create.core.ZipUtils;

public class ProjectStructureValidator {

    public IStatus validate(Path path, IProgressMonitor monitor) {
        if (path == null) {
            return error(new AssertionFailedException("'path' must not be null."));
        }
        System.out.println(path + ": " + ZipUtils.isZip(path));

        SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
        try {
            Files.walkFileTree(path, new ValidatorVisitor(path, subMonitor));
        } catch (IOException e) {
            return error(e);
        } finally {
            if (monitor != null) {
                monitor.done();
            }
        }
        return OK_STATUS;
    }

    private static final class ValidatorVisitor extends SimpleFileVisitor<Path> {

        private final SubMonitor monitor;
        private final Path root;

        private ValidatorVisitor(Path root, SubMonitor monitor) throws IOException {
            this.root = isZip(root) ? unzip(root, Files.createTempDirectory(null)) : root;
            this.monitor = monitor;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            monitor.beginTask("Validating project structure...", 1);

            // CMakeLists.txt
            Path cmakeLists = root.resolve("CMakeLists.txt");
            if (!isReadableFile(cmakeLists)) {
                throw new IOException("Couldn't find 'CMakeLists.txt' file.");
            }
            monitor.worked(1);

            // The `sketch` folder and it's content
            Path sketch = root.resolve("sketch");
            if (!isReadableDirectory(sketch)) {
                throw new IOException("Couldn't find the 'sketch' folder.");
            }
            if (list(sketch).filter(p -> p.toString().endsWith(".ino.cpp")).count() < 1) {
                throw new IOException("Couldn't find any sketch files in the 'sketch' folder.");
            }
            monitor.worked(1);

            // The `core` folder but no content
            Path core = root.resolve("core");
            if (!isReadableDirectory(core)) {
                throw new IOException("Couldn't find the 'core' folder.");
            }
            monitor.worked(1);

            return FileVisitResult.SKIP_SUBTREE;
        }

        private boolean isReadableFile(Path path) {
            return exists(path) && isRegularFile(path) && isReadable(path);
        }

        private boolean isReadableDirectory(Path path) {
            return exists(path) && isDirectory(path) && isReadable(path);
        }
    }

}
