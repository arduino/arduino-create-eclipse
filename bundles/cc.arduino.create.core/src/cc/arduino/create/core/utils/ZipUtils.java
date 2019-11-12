/**
 * Copyright (C) 2019 Arduino SA and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package cc.arduino.create.core.utils;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.notExists;
import static java.nio.file.Files.walkFileTree;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.core.runtime.Assert;

public final class ZipUtils {

    public static boolean isZip(Path path) {
        Assert.isNotNull(path, "path");

        if (!exists(path) || isDirectory(path)) {
            return false;
        }
        int signature = -1;
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            signature = raf.readInt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return signature == 0x504B0304;
    }

    public static Path unzip(Path zip, Path target) {
        Assert.isNotNull(zip, "zip");
        Assert.isNotNull(target, "target");
        Assert.isLegal(exists(zip), zip + " does not exist");
        Assert.isLegal(isZip(zip), zip + " is not a ZIP file");
        Assert.isLegal(!exists(target) || isDirectory(target), "target cannot be an existing file");

        try {

            if (notExists(target)) {
                createDirectories(target);
            }

            try (FileSystem zipFileSystem = FileSystems.newFileSystem(zip, null)) {
                final Path root = zipFileSystem.getRootDirectories().iterator().next();
                walkFileTree(root, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        final Path destFile = Paths.get(target.toString(), file.toString());
                        try {
                            Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                        } catch (DirectoryNotEmptyException ignore) {
                            // NOOP
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        final Path dirToCreate = Paths.get(target.toString(), dir.toString());
                        if (notExists(dirToCreate)) {
                            createDirectory(dirToCreate);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                });
            }

            return target;
        } catch (IOException e) {
            throw new RuntimeException("Error when unzipping " + zip + " to " + target, e);
        }
    }

    private ZipUtils() {
    }

}
