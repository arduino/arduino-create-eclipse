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
import java.nio.file.Path;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IStatus;
import org.junit.Test;

public class ProjectStructureValidatorTest {

    private final ProjectStructureValidator validator = new ProjectStructureValidator();

    @Test(expected = AssertionFailedException.class)
    public void check_null() {
        validator.validate(null, null);
    }

    @Test
    public void check_empty() {
        IStatus status = validator.validate(path("empty"), null);
        assertEquals(ERROR, status.getSeverity());
    }

    @Test
    public void check_noSketchFolder() {
        IStatus status = validator.validate(path("noSketchFolder"), null);
        assertEquals(ERROR, status.getSeverity());
    }

    @Test
    public void check_noSketchFile() {
        IStatus status = validator.validate(path("noSketchFile"), null);
        assertEquals(ERROR, status.getSeverity());
    }

    @Test
    public void check_noCMakeLists() {
        IStatus status = validator.validate(path("noCMakeLists"), null);
        assertEquals(ERROR, status.getSeverity());
    }

    @Test
    public void check_valid() {
        IStatus status = validator.validate(path("valid"), null);
        assertEquals(OK, status.getSeverity());
    }

    private Path path(String other) {
        return new File("").getAbsoluteFile().toPath().resolve("resources").resolve(other);
    }

}
