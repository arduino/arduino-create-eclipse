/**
 * Copyright (C) 2019 Arduino SA and others.
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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.runtime.IStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ProjectStructureValidatorTest {

    private final ProjectStructureValidator validator = new ProjectStructureValidator();

    @Parameters(name = "{index}: validate({0})={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { null, ERROR },
                { path("missing"), ERROR },
                { path("empty"), ERROR },
                { path("noCoreFolder"), ERROR },
                { path("noSketchFolder"), ERROR },
                { path("noSketchFile"), ERROR },
                { path("noCMakeLists"), ERROR },
                { path("valid"), OK },
                { path("empty.zip"), ERROR },
                { path("noCoreFolder.zip"), ERROR },
                { path("noSketchFolder.zip"), ERROR },
                { path("noSketchFile.zip"), ERROR },
                { path("noCMakeLists.zip"), ERROR },
                { path("no_cmake.zip"), "'_cmake' is missing from the root of the archive." },
                { path("valid.zip"), OK }
        });
    }

    @Parameter(0)
    public Path path;

    /**
     * The expected {@link IStatus#getSeverity() severity} as an {@code Integer}, or the {@link IStatus#getMessage()
     * message} of the validation error.
     */
    @Parameter(1)
    public Object expected;

    @Test
    public void test() {
        IStatus status = validator.validate(path, null);
        if (expected instanceof Integer) {
            assertEquals(expected, status.getSeverity());
        } else if (expected instanceof String) {
            assertEquals("Expected an ERROR when the validation message is defined.", ERROR, status.getSeverity());
            assertTrue(status.getMessage().contains(String.valueOf(expected)));
        } else {
            throw new UnsupportedOperationException("Implementation error. Unexpected test expecation.");
        }
    }

    private static Path path(String other) {
        return new File("").getAbsoluteFile().toPath().resolve("resources").resolve(other);
    }

}
