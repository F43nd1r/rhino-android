package org.mozilla.javascript.benchmarks;

import android.support.test.InstrumentationRegistry;

import com.faendir.rhino_android.RhinoAndroidHelper;

import org.junit.AfterClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.drivers.TestUtils;
import org.mozilla.javascript.tools.shell.Global;

import java.io.*;
import java.util.HashMap;

public class V8Benchmark
{
    public static final String TEST_SRC = "benchmarks/v8-benchmarks-v6/run.js";

    private static final HashMap<Integer, String> results = new HashMap<Integer, String>();

    @AfterClass
    public static void writeResults()
        throws IOException
    {
        PrintWriter out = new PrintWriter(
                new FileWriter(new File(InstrumentationRegistry.getContext().getFilesDir(), "v8benchmark.csv"))
        );
        // Hard code the opt levels for now -- we will make it more generic when we need to
        out.println("Optimization 0,Optimization 9");
        out.println(results.get(0) + ',' + results.get(9));
        out.close();
    }

    private void runTest(int optLevel)
        throws IOException
    {


        Context cx = RhinoAndroidHelper.prepareContext();
        cx.setLanguageVersion(Context.VERSION_1_8);
        cx.setOptimizationLevel(optLevel);
        Global root = new Global(cx);
        TestUtils.addAssetLoading(root);
        root.put("RUN_NAME", root, "V8-Benchmark-" + optLevel);
        Object result = cx.evaluateString(root, TestUtils.readAsset(TEST_SRC), TEST_SRC, 1, null);
        results.put(optLevel, result.toString());
    }

    @Test
    public void testOptLevel9()
        throws IOException
    {
        runTest(9);
    }

    @Test
    public void testOptLevel0()
        throws IOException
    {
        runTest(0);
    }
}
