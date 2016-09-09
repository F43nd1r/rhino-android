package com.faendir.rhino_android;

import android.support.annotation.VisibleForTesting;

import org.mozilla.javascript.ContextFactory;

import java.io.File;

/**
 * Ensures that the classLoader used is correct
 *
 * @author F43nd1r
 * @since 11.01.2016
 */
@VisibleForTesting
public class AndroidContextFactory extends ContextFactory {

    private final File cacheDirectory;

    /**
     * Create a new factory. It will cache generated code in the given directory
     *
     * @param cacheDirectory the cache directory
     */
    public AndroidContextFactory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        initApplicationClassLoader(createClassLoader(AndroidContextFactory.class.getClassLoader()));
    }

    /**
     * Create a ClassLoader which is able to deal with bytecode
     *
     * @param parent the parent of the create classloader
     * @return a new ClassLoader
     */
    @Override
    protected AndroidClassLoader createClassLoader(ClassLoader parent) {
        return new AndroidClassLoader(parent, cacheDirectory);
    }
}
