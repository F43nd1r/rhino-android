package com.faendir.rhino_android;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.SecurityController;

/**
 * Created by Lukas on 11.01.2016.
 *
 * Helps to prepare a Rhino Context for usage on android.
 */
public final class RhinoAndroidHelper {
    private RhinoAndroidHelper() {
    }

    /**
     * call this instead of Context.enter()
     * @return a context prepared for android
     */
    public static Context prepareContext(){
        if(!SecurityController.hasGlobal())SecurityController.initGlobal(new NoSecurityController());
        if(!ContextFactory.hasExplicitGlobal()){
            ContextFactory.getGlobalSetter().setContextFactoryGlobal(new AndroidContextFactory());
        }
        else if (!(ContextFactory.getGlobal() instanceof AndroidContextFactory)) {
            throw new IllegalStateException("Cannot initialize factory for Android Rhino: There is already another factory");
        }
        return Context.enter();
    }
}
