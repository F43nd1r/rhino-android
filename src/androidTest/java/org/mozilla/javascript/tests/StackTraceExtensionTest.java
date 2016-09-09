package org.mozilla.javascript.tests;

import com.faendir.rhino_android.AndroidContextFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.StackStyle;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.shell.Global;

import java.io.File;

public class StackTraceExtensionTest
{
    @BeforeClass
    public static void init()
    {
        RhinoException.setStackStyle(StackStyle.V8);
    }

    @AfterClass
    public static void terminate()
    {
        RhinoException.setStackStyle(StackStyle.RHINO);
    }

    private void testTraces(int opt)
    {
        final ContextFactory factory = new AndroidContextFactory(new File(System.getProperty("java.io.tmpdir", "."), "classes")) {
            @Override
            protected boolean hasFeature(Context cx, int featureIndex)
            {
                switch (featureIndex) {
                case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR:
                    return true;
                default:
                    return super.hasFeature(cx, featureIndex);
                }
            }
        };

        Context cx = factory.enterContext();
        try {
            cx.setLanguageVersion(Context.VERSION_1_8);
            cx.setOptimizationLevel(opt);
            cx.setGeneratingDebug(true);

            Global global = new Global(cx);
            TestUtils.addAssetLoading(global);
            Scriptable root = cx.newObject(global);


            cx.evaluateString(root, TestUtils.readAsset("jstests/extensions/stack-traces.js"), "stack-traces.js", 1, null);
        } finally {
            Context.exit();
        }
    }

    @Test
    public void testStackTrace0()
    {
        testTraces(0);
    }

    @Test
    public void testStackTrace9()
    {
        testTraces(9);
    }

    @Test
    public void testStackTraceInt()
    {
        testTraces(-1);
    }
}
