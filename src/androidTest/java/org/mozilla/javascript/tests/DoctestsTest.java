/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import com.faendir.rhino_android.RhinoAndroidHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.shell.Global;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Run doctests in folder assets/doctests.
 *
 * A doctest is a test in the form of an interactive shell session; Rhino
 * collects and runs the inputs to the shell prompt and compares them to the
 * expected outputs.
 *
 * @author Norris Boyd
 */
@RunWith(Parameterized.class)
public class DoctestsTest {
    static final String baseDirectory = "doctests";
    static final String doctestsExtension = ".doctest";
    String name;
    String source;
    int optimizationLevel;

    public DoctestsTest(String name, String source, int optimizationLevel) {
        this.name = name;
        this.source = source;
        this.optimizationLevel = optimizationLevel;
    }

    public static File[] getDoctestFiles() {
        return TestUtils.recursiveListAssets(new File(baseDirectory),
                new FileFilter() {
                    public boolean accept(File f) {
                        return f.getName().endsWith(doctestsExtension);
                    }
            });
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> doctestValues() throws IOException {
        File[] doctests = getDoctestFiles();
        List<Object[]> result = new ArrayList<Object[]>();
        for (File f : doctests) {
            String contents = TestUtils.readAsset(f);
            result.add(new Object[] { f.getName(), contents, -1 });
            result.add(new Object[] { f.getName(), contents, 0 });
            result.add(new Object[] { f.getName(), contents, 9 });
        }
        return result;
    }

    // move "@Parameters" to this method to test a single doctest
    public static Collection<Object[]> singleDoctest() throws IOException {
        List<Object[]> result = new ArrayList<Object[]>();
        File f = new File(baseDirectory, "Counter.doctest");
        String contents = TestUtils.readAsset(f);
        result.add(new Object[] { f.getName(), contents, -1 });
        return result;
    }

    @Test
    public void runDoctest() throws Exception {
        Context cx = RhinoAndroidHelper.prepareContext();
        try {
            cx.setOptimizationLevel(optimizationLevel);
            Global global = new Global(cx);
            TestUtils.addAssetLoading(global);
            // global.runDoctest throws an exception on any failure
            int testsPassed = global.runDoctest(cx, global, source, name, 1);
            System.out.println(name + "(" + optimizationLevel + "): " +
                    testsPassed + " passed.");
            assertTrue(testsPassed > 0);
        } catch (Exception ex) {
          System.out.println(name + "(" + optimizationLevel + "): FAILED due to "+ex);
          throw ex;
        } finally {
            Context.exit();
        }
    }
}
