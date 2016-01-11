package com.faendir.rhino_android;

import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.GeneratedClassLoader;

/**
 * Created by Lukas on 11.01.2016.
 * Ensures that the classLoader used is correct
 */
class AndroidContextFactory extends ContextFactory{
    @Override
    protected GeneratedClassLoader createClassLoader(ClassLoader parent) {
        return new AndroidClassLoader(parent);
    }
}
