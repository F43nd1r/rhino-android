package org.mozilla.javascript.tests;

import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest(
    value = "jstests/inside-strict-mode.js"
)
public class InsideStrictModeTest
    extends ScriptTestsBase
{
}
