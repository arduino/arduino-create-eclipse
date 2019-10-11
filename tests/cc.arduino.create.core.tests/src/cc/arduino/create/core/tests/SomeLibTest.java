/**
 * Copyright (C) 2019 TypeFox and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package cc.arduino.create.core.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import cc.arduino.create.core.SomeLib;

/**
 *
 */
public class SomeLibTest {

    @Test
    public void testFoo_01() {
        assertTrue(new SomeLib().foo());
    }

    @Test
    @Ignore
    public void testFoo_02() {
        assertFalse(new SomeLib().foo());
    }

}
