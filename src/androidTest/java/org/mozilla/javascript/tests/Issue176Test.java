/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import com.faendir.rhino_android.RhinoAndroidHelper;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.drivers.TestUtils;

public class Issue176Test extends TestCase {


    Context cx;
    Scriptable scope;

    public void testThrowing() throws Exception {
        cx = RhinoAndroidHelper.prepareContext();
        try {
            Script script = cx.compileString(TestUtils.readAsset("Issue176.js"),
                    "Issue176.js", 1, null);
            scope = cx.initStandardObjects();
            scope.put("host", scope, this);
            script.exec(cx, scope); // calls our methods
        } finally {
            Context.exit();
        }
    }


    public void throwError(String msg) {
        throw ScriptRuntime.throwError(cx, scope, msg);
    }


    public void throwCustomError(String constr, String msg) {
        throw ScriptRuntime.throwCustomError(cx, scope, constr, msg);
    }

}
