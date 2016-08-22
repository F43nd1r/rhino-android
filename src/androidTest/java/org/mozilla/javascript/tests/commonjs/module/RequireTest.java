package org.mozilla.javascript.tests.commonjs.module;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.commonjs.module.ModuleScript;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.drivers.TestUtils;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Attila Szegedi
 * @version $Id: RequireTest.java,v 1.1 2011/04/07 22:24:37 hannes%helma.at Exp $
 */
public class RequireTest extends TestCase {
    private final File dir = new File("org/mozilla/javascript/tests/commonjs/module");

    public void testSandboxed() throws Exception {
        final Context cx = createContext();
        final Require require = getSandboxedRequire(cx);
        require.requireMain(cx, "testSandboxed");
        // Also, test idempotent double-require of same main:
        require.requireMain(cx, "testSandboxed");
        // Also, test failed require of different main:
        try {
            require.requireMain(cx, "blah");
            fail();
        } catch (IllegalStateException e) {
            // Expected, success
        }
    }

    private Context createContext() {
        final Context cx = Context.enter();
        cx.setOptimizationLevel(-1);
        return cx;
    }

    public void testNonSandboxed() throws Exception {
        final Context cx = createContext();
        final Scriptable scope = cx.initStandardObjects();
        final Require require = getSandboxedRequire(cx, scope, false);
        final String jsFile = TestUtils.readAsset(dir + File.separator + "testNonSandboxed.js");
        ScriptableObject.putProperty(scope, "moduleUri", jsFile);
        require.requireMain(cx, "testNonSandboxed");
    }

    public void testVariousUsageErrors() throws Exception {
        testWithSandboxedRequire("testNoArgsRequire");
    }

    public void testRelativeId() throws Exception {
        final Context cx = createContext();
        final Scriptable scope = cx.initStandardObjects();
        final Require require = getSandboxedRequire(cx, scope, false);
        require.install(scope);
        cx.evaluateString(scope, TestUtils.readAsset(dir + File.separator + "testRelativeId.js"),
                "testRelativeId.js", 1, null);
    }

    public void testSetMainForAlreadyLoadedModule() throws Exception {
        final Context cx = createContext();
        final Scriptable scope = cx.initStandardObjects();
        final Require require = getSandboxedRequire(cx, scope, false);
        require.install(scope);
        cx.evaluateString(scope, TestUtils.readAsset(dir + File.separator + "testSetMainForAlreadyLoadedModule.js"),
                "testSetMainForAlreadyLoadedModule.js", 1, null);
        try {
            require.requireMain(cx, "assert");
            fail();
        } catch (IllegalStateException e) {
            assertEquals(e.getMessage(), "Attempt to set main module after it was loaded");
        }
    }

    private Reader getReader(String name) {
        return new InputStreamReader(getClass().getResourceAsStream(name));
    }

    private void testWithSandboxedRequire(String moduleId) throws Exception {
        final Context cx = createContext();
        getSandboxedRequire(cx).requireMain(cx, moduleId);
    }

    private Require getSandboxedRequire(final Context cx)
            throws URISyntaxException {
        return getSandboxedRequire(cx, cx.initStandardObjects(), true);
    }

    private Require getSandboxedRequire(Context cx, Scriptable scope, boolean sandboxed)
            throws URISyntaxException {
        return new Require(cx, cx.initStandardObjects(),
                new ModuleScriptProvider() {
                    @Override
                    public ModuleScript getModuleScript(Context cx, String moduleId, URI moduleUri, URI baseUri, Scriptable paths) throws Exception {
                        File f = new File(dir, moduleId + ".js");
                        return new ModuleScript(cx.compileString(TestUtils.readAsset(f), moduleId, 1, null), f.toURI(), dir.toURI());
                    }
                }, null, null, true);
    }

    private URI getDirectory() throws URISyntaxException {
        final String jsFile = TestUtils.readAsset(getClass().getPackage().getName() + File.separator + "testSandboxed.js");
        return new URI(jsFile);
    }
}
