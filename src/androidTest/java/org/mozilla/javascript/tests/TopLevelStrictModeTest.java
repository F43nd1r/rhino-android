package org.mozilla.javascript.tests;

import org.junit.Ignore;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest(
    value = "testsrc/jstests/top-level-strict-mode.js"
)
@Ignore
public class TopLevelStrictModeTest
    extends ScriptTestsBase
{
}
