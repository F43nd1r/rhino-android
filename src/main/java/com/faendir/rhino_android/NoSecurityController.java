package com.faendir.rhino_android;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.SecurityController;

import java.io.Serializable;

/**
 * The concept of SecurityControllers doesn't make sense on Android.
 * Load this controller to prevent loading of a different one.
 *
 * @author F43nd1r
 * @since 11.01.2016
 */
class NoSecurityController extends SecurityController implements Serializable {
    @Override
    public GeneratedClassLoader createClassLoader(ClassLoader classLoader, Object o) {
        return Context.getCurrentContext().createClassLoader(classLoader);
    }

    @Override
    public Object getDynamicSecurityDomain(Object o) {
        return null;
    }
}
