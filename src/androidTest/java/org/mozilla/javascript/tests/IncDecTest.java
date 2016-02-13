package org.mozilla.javascript.tests;

import org.junit.Ignore;
import org.mozilla.javascript.drivers.RhinoTest;
import org.mozilla.javascript.drivers.ScriptTestsBase;

@RhinoTest(
    value = "testsrc/jstests/inc-dec.js"
)
@Ignore
public class IncDecTest
    extends ScriptTestsBase
{
}
