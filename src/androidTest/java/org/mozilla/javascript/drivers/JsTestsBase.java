/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.drivers;

import com.faendir.rhino_android.RhinoAndroidHelper;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.IOException;

public abstract class JsTestsBase extends TestCase {
    private int optimizationLevel;

    public void setOptimizationLevel(int level) {
        this.optimizationLevel = level;
    }

    public void runJsTest(Context cx, Scriptable shared, String name, String source) {
        // create a lightweight top-level scope
        Scriptable scope = cx.newObject(shared);
        scope.setPrototype(shared);
        System.out.print(name + ": ");
        Object result;
        try {
            result = cx.evaluateString(scope, source, "jstest input: " + name, 1, null);
        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            System.out.println("FAILED");
            throw e;
        }
        assertTrue(result != null);
        assertTrue("success".equals(result));
        System.out.println("passed");
    }

    public void runJsTests(File[] tests) throws IOException {
        Context cx = RhinoAndroidHelper.prepareContext();
        try {
            cx.setOptimizationLevel(this.optimizationLevel);
            Scriptable shared = cx.initStandardObjects();
            for (File f : tests) {
                String session = TestUtils.readAsset(f);
                runJsTest(cx, shared, f.getName(), session);
            }
        } finally {
            Context.exit();
        }
    }
}
