package org.mozilla.javascript.tests.harmony;

import org.junit.Ignore;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest(
    value = "testsrc/jstests/harmony/parse-int-float.js"
)
@Ignore
public class ParseIntFloatTest
    extends ScriptTestsBase
{
}
