/*******************************************************************************
 * Copyright (c) 2007, 2010 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
// Based on https://github.com/eclipse-cdt/cdt/blob/170e654b4796bad1453ae85a427b97317d67a69a/build/org.eclipse.cdt.managedbuilder.ui/src/org/eclipse/cdt/managedbuilder/ui/wizards/AbstractCWizard.java
/**
 * Copyright (C) 2019 Arduino SA and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package cc.arduino.create.core.managedbuilders;

import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;

public final class ToolChainUtils {

    public interface Options {

        String[] getLanguageIDs();

        String[] getContentTypeIDs();

        String[] getExtensions();

        /**
         * Default options for C/C++.
         */
        // https://github.com/eclipse-cdt/cdt/blob/170e654b4796bad1453ae85a427b97317d67a69a/core/org.eclipse.cdt.core/plugin.xml#L520-L529
        // https://github.com/eclipse-cdt/cdt/blob/170e654b4796bad1453ae85a427b97317d67a69a/build/org.eclipse.cdt.managedbuilder.gnu.ui/plugin.xml#L1695-L1703
        Options DEFAULT = new Options() {

            @Override
            public String[] getLanguageIDs() {
                return new String[] { "org.eclipse.cdt.core.g++" };
            }

            @Override
            public String[] getExtensions() {
                return new String[] { "C", "cpp", "cxx", "cc", "c++", "h", "hpp", "hh", "hxx", "inc" };
            }

            @Override
            public String[] getContentTypeIDs() {
                return new String[] { "org.eclipse.cdt.core.cxxSource", "org.eclipse.cdt.core.cxxHeader" };
            }

        };
    }

    public static boolean isValid(IToolChain tc, boolean supportedOnly, Options options) {
        // Check for language compatibility first in any case
        if (!isLanguageCompatible(tc, options))
            return false;

        // Do not do further check if all toolchains are permitted
        if (!supportedOnly)
            return true;

        // Filter off unsupported and system toolchains
        if (tc == null || !tc.isSupported() || tc.isAbstract() || tc.isSystemObject())
            return false;

        // Check for platform compatibility
        return ManagedBuildManager.isPlatformOk(tc);
    }

    private static boolean isLanguageCompatible(IToolChain tc, Options options) {
        if (options == null)
            return true;

        ITool[] tools = tc.getTools();
        String[] langIDs = options.getLanguageIDs();
        String[] ctypeIDs = options.getContentTypeIDs();
        String[] exts = options.getExtensions();

        // nothing required?
        if (empty(langIDs) && empty(ctypeIDs) && empty(exts))
            return true;

        for (int i = 0; i < tools.length; i++) {
            IInputType[] its = tools[i].getInputTypes();

            // no input types - check only extensions
            if (empty(its)) {
                if (!empty(exts)) {
                    String[] s = tools[i].getAllInputExtensions();
                    if (contains(exts, s))
                        return true; // extension fits
                }
                continue;
            }
            // normal tool with existing input type(s)
            for (int j = 0; j < its.length; j++) {
                // Check language IDs
                if (!empty(langIDs)) {
                    String lang = its[j].getLanguageId(tools[i]);
                    if (contains(langIDs, new String[] { lang })) {
                        return true; // Language ID fits
                    }
                }
                // Check content types
                if (!empty(ctypeIDs)) {
                    String[] ct1 = its[j].getSourceContentTypeIds();
                    String[] ct2 = its[j].getHeaderContentTypeIds();
                    if (contains(ctypeIDs, ct1) || contains(ctypeIDs, ct2)) {
                        return true; // content type fits
                    }
                }
                // Check extensions
                if (!empty(exts)) {
                    String[] ex1 = its[j].getHeaderExtensions(tools[i]);
                    String[] ex2 = its[j].getSourceExtensions(tools[i]);
                    if (contains(exts, ex1) || contains(exts, ex2)) {
                        return true; // extension fits fits
                    }
                }
            }
        }
        return false; // no one value fits to required
    }

    private static boolean empty(Object[] s) {
        return (s == null || s.length == 0);
    }

    private static boolean contains(String[] s1, String[] s2) {
        for (int i = 0; i < s1.length; i++)
            for (int j = 0; j < s2.length; j++)
                if (s1[i].equals(s2[j]))
                    return true;
        return false;
    }

    private ToolChainUtils() {
    }

}
