package com.faendir.rhino_android;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.SecurityController;

/**
 * Created by Lukas on 11.01.2016.
 *
 * The concept of SecurityControllers doesn't make sense on Android.
 * Load this controller to prevent loading of a different one.
 */
class NoSecurityController extends SecurityController {
    @Override
    public GeneratedClassLoader createClassLoader(ClassLoader classLoader, Object o) {
        return Context.getCurrentContext().createClassLoader(classLoader);
    }

    @Override
    public Object getDynamicSecurityDomain(Object o) {
        return null;
    }
}
