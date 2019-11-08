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

import static org.eclipse.core.runtime.IStatus.ERROR;
import static org.eclipse.core.runtime.IStatus.OK;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import cc.arduino.create.core.utils.ZipUtils;

@RunWith(Parameterized.class)
public class ProjectStructureValidatorTest {

    private final ProjectStructureValidator validator = new ProjectStructureValidator();

    @Parameters(name = "{index}: validate({0})={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { null, ERROR },
                { path("missing"), ERROR },
                { path("noReadAccess"), ERROR }, // `sudo chmod -R 333 ./noReadAccess/`
                { path("empty"), ERROR },
                { path("noCoreFolder"), ERROR },
                { path("noSketchFolder"), ERROR },
                { path("noSketchFile"), ERROR },
                { path("noCMakeLists"), ERROR },
                { path("valid"), OK },
                { path("arduino_create-cmake.zip"), OK },
                { zip("empty"), ERROR },
                { zip("noCoreFolder"), ERROR },
                { zip("noSketchFolder"), ERROR },
                { zip("noSketchFile"), ERROR },
                { zip("noCMakeLists"), ERROR },
                // { zip("valid"), OK }
        });
    }

    @Parameter(0)
    public Path path;

    @Parameter(1)
    public int expected;

    @Test
    public void test() {
        assertEquals(expected, validator.validate(path, null).getSeverity());
    }

    private static Path path(String other) {
        return new File("").getAbsoluteFile().toPath().resolve("resources").resolve(other);
    }

    private static Path zip(String other) {
        try {
            Path target = Files.createTempFile(other, ".zip");
            Path source = path(other);
            return ZipUtils.zip(source, target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
