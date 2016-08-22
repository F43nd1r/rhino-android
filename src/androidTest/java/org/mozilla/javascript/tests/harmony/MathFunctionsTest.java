package org.mozilla.javascript.tests.harmony;

import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest(
    value = "jstests/harmony/math-functions.js"
)
public class MathFunctionsTest
    extends ScriptTestsBase
{
}
