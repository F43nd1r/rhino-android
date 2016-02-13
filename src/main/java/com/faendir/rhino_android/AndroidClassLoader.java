package com.faendir.rhino_android;

import com.android.dx.command.dexer.Main;

import org.mozilla.javascript.GeneratedClassLoader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import dalvik.system.DexFile;

/**
 * Created by Lukas on 11.01.2016.
 * <p/>
 * Parses java bytecode to dex bytecode and loads it
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
class AndroidClassLoader extends ClassLoader implements GeneratedClassLoader {

    private final ClassLoader parent;
    private DexFile dx;
    private final File classFile;
    private final File dexFile;
    private final File odexOatFile;

    public AndroidClassLoader(ClassLoader parent) {
        this.parent = parent;
        File dir = new File(System.getProperty("java.io.tmpdir", "."), "classes");
        classFile = new File(dir, "class-" + hashCode() + ".jar");
        dexFile = new File(dir, "dex-" + hashCode() + ".jar");
        odexOatFile = new File(dir, "odex_oat-" + hashCode() + ".tmp");
        dir.mkdirs();
    }


    public Class<?> defineClass(String name, byte[] data) {
        JarOutputStream out = null;
        try {
            out = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(classFile)));
            out.putNextEntry(new JarEntry(name.replace('.', '/') + ".class"));
            out.write(data);
            out.flush();
            out.close();
            Main.main(new String[]{"--output=" + dexFile.getPath(), classFile.getPath()});
            dx = DexFile.loadDex(dexFile.getPath(), odexOatFile.getPath(), 0);
            return dx.loadClass(name, parent);
        } catch (IOException e) {
            throw new FatalLoadingException(e);
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            classFile.delete();
            dexFile.delete();
            odexOatFile.delete();
        }
    }

    @Override
    public void linkClass(Class<?> aClass) {
        //doesn't make sense on android
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class<?> cl = findLoadedClass(name);
        if (cl == null) {
            if (dx != null) {
                cl = dx.loadClass(name, parent);
            } else {
                cl = findSystemClass(name);
            }
        }
        return cl;
    }

    private class FatalLoadingException extends RuntimeException {
        public FatalLoadingException(Throwable t) {
            super("Failed to define class", t);
        }
    }
}
