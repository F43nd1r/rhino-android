/*
 * Copyright (c) 2016 Lukas Morawietz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.faendir.rhino_android;

import android.support.annotation.VisibleForTesting;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.SecurityController;

import java.io.File;
import java.io.IOException;

/**
 * Helps to prepare a Rhino Context for usage on android.
 *
 * @author F43nd1r
 * @since 11.01.2016
 */
public class RhinoAndroidHelper {

    private final File cacheDirectory;

    /**
     * Constructs a new helper using the default temporary directory.
     * Note: It is recommended to use a custom directory, so no permission problems occur.
     */
    public RhinoAndroidHelper() {
        this(new File(System.getProperty("java.io.tmpdir", "."), "classes"));
    }

    /**
     * Constructs a new helper using a directory in the applications cache.
     *
     * @param context any context
     */
    public RhinoAndroidHelper(android.content.Context context) {
        this(new File(context.getCacheDir(), "classes"));
    }

    /**
     * Constructs a helper using the specified directory as cache.
     *
     * @param cacheDirectory the cache directory to use
     */
    public RhinoAndroidHelper(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    /**
     * call this instead of {@link Context#enter()}
     *
     * @return a context prepared for android
     */
    public Context enterContext() {
        if (!SecurityController.hasGlobal())
            SecurityController.initGlobal(new NoSecurityController());
        return getContextFactory().enterContext();
    }

    /**
     * @return The Context factory which has to be used on android.
     */
    @VisibleForTesting
    public AndroidContextFactory getContextFactory() {
        AndroidContextFactory factory;
        if (!ContextFactory.hasExplicitGlobal()) {
            factory = new AndroidContextFactory(cacheDirectory);
            ContextFactory.getGlobalSetter().setContextFactoryGlobal(factory);
        } else if (!(ContextFactory.getGlobal() instanceof AndroidContextFactory)) {
            throw new IllegalStateException("Cannot initialize factory for Android Rhino: There is already another factory");
        } else {
            factory = (AndroidContextFactory) ContextFactory.getGlobal();
        }
        return factory;
    }

    /**
     * Compiles all classes in a jar file. They will be available in Rhino.
     *
     * @param jar the jar to load
     * @throws IOException if the jar cannot be read or is invalid or there is a problem with the cache
     */
    public void loadClassJar(File jar) throws IOException {
        ((AndroidClassLoader) getContextFactory().getApplicationClassLoader()).loadJar(jar);
    }

    /**
     * @return a context prepared for android
     * @deprecated use {@link #enterContext()} instead
     */
    @Deprecated
    public static Context prepareContext() {
        return new RhinoAndroidHelper().enterContext();
    }
}
