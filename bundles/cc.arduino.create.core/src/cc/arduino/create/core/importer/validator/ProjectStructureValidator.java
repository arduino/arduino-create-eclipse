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
import static cc.arduino.create.core.utils.ZipUtils.isZip;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isReadable;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.walkFileTree;
import static org.eclipse.core.runtime.Status.OK_STATUS;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import com.google.common.base.Function;

public class ProjectStructureValidator {

    public IStatus validate(Path path, IProgressMonitor monitor) {
        try {
            SubMonitor subMonitor = SubMonitor.convert(monitor, "Validating project structure...", 4);
            if (path == null) {
                return error(new AssertionFailedException("Path must be specified."));
            }
            Path normalized = path.toAbsolutePath().normalize();
            if (!exists(normalized)) {
                return error(new AssertionFailedException("'" + path + "' does exist."));
            }
            if (!isReadable(normalized)) {
                return error(new AssertionFailedException("'" + path + "' is not accessible."));
            }
            subMonitor.worked(1);

            ValidatorVisitor visitor;
            if (isZip(normalized)) {
                try (FileSystem fs = FileSystems.newFileSystem(normalized, null)) {
                    Path root = fs.getRootDirectories().iterator().next();
                    visitor = new ValidatorVisitor(root, subMonitor.newChild(3));
                    walkFileTree(root, visitor);
                }
            } else {
                visitor = new ValidatorVisitor(normalized, subMonitor.newChild(3));
                walkFileTree(normalized, visitor);
            }
            IStatus result = visitor.getResult();
            if (!result.isOK()) {
                throw new IOException(result.getMessage());
            }
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

        private final Path root;
        private final SubMonitor monitor;
        private final Map<Function<Path, Boolean>, IStatus> constraints = new LinkedHashMap<>();

        {
            // Validates the existence of the `CMakeLists.txt` file in the root.
            constraints.put(
                    path -> isPathAt("CMakeLists.txt", path) && isReadableFile(path),
                    error("Invalid project structure. Couldn't find 'CMakeLists.txt' file."));
            // Validates the existence of the `sketch` folder and its content in the root.
            constraints.put(path -> {
                if (isPathAt("sketch", path) && isReadableDirectory(path)) {
                    try {
                        return Files.list(path).filter(p -> p.toString().endsWith(".ino.cpp")).count() > 0;
                    } catch (IOException e) {
                        // Ignored
                    }
                }
                return false;
            }, error("Invalid project structure. Couldn't locate any sketch files inside the 'sketch' folder."));
            // Validates the existence of the `core` folder in the root.
            constraints.put(
                    path -> isPathAt("core", path) && isReadableDirectory(path),
                    error("Invalid project structure. Couldn't find the 'core' folder."));
        }

        private ValidatorVisitor(Path root, SubMonitor monitor) {
            this.root = root;
            this.monitor = monitor;
            monitor.beginTask("Validating project structure...", 1);
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            validate(dir);
            return super.preVisitDirectory(dir, attrs);
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            validate(file);
            return constraints.isEmpty() ? SKIP_SUBTREE : CONTINUE;
        }

        public IStatus getResult() {
            if (constraints.isEmpty()) {
                return Status.OK_STATUS;
            }
            return constraints.values().iterator().next();
        }

        private boolean isReadableFile(Path path) {
            return exists(path) && isRegularFile(path) && isReadable(path);
        }

        private boolean isReadableDirectory(Path path) {
            return exists(path) && isDirectory(path) && isReadable(path);
        }

        private void validate(Path file) {
            Iterator<Function<Path, Boolean>> itr = constraints.keySet().iterator();
            while (itr.hasNext()) {
                Function<Path, Boolean> predicate = itr.next();
                if (predicate.apply(file)) {
                    itr.remove();
                    monitor.worked(1);
                }
            }
        }

        private boolean isPathAt(String expected, Path path) {
            if (path == null || path.getFileName() == null) {
                return false;
            }
            String actual = relativeFromRoot(resolveFromRoot(path.getFileName().toString())).toString();
            if (actual.startsWith("/")) {
                actual = actual.substring(1);
            }
            if (actual.endsWith("/")) {
                actual = actual.substring(0, actual.length() - 1);
            }
            return expected.equals(actual);
        }

        private Path relativeFromRoot(Path path) {
            return root.relativize(path);
        }

        private Path resolveFromRoot(String path) {
            return root.resolve(path);
        }

    }

}
