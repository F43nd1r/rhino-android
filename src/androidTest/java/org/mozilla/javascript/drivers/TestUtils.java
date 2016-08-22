/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.drivers;

import android.content.res.AssetManager;
import android.support.test.InstrumentationRegistry;

import com.google.common.io.ByteStreams;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class TestUtils {

    public static File[] recursiveListAssets(File dir, FileFilter filter) {
        List<File> fileList = new ArrayList<File>();
        recursiveListAssetsHelper(dir, filter, fileList);
        return fileList.toArray(new File[fileList.size()]);
    }

    public static String readAsset(File file){
        return readAsset(file.getPath());
    }

    public static String readAsset(String file) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(InstrumentationRegistry.getContext().getAssets().open(file)));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            if(builder.length() > 0)builder.deleteCharAt(builder.length() - 1);
        } catch (IOException ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return builder.toString();
    }

    public static void recursiveListAssetsHelper(File dir, FileFilter filter,
                                                 List<File> fileList) {
        AssetManager assetManager = InstrumentationRegistry.getContext().getAssets();
        try {
            String[] list = assetManager.list(dir.getPath());
            if (list != null) {
                for (String s : list) {
                    File f = new File(dir, s);
                    recursiveListAssetsHelper(f, filter, fileList);
                    if (filter.accept(f))
                        fileList.add(f);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static List<File> listAssetDirectories(String dir) {
        List<File> out = new ArrayList<>();
        AssetManager assetManager = InstrumentationRegistry.getContext().getAssets();
        try {
            String[] list = assetManager.list(dir);
            if (list != null) {
                for (String s : list) {
                    File f = new File(dir, s);
                    String[] sub = assetManager.list(f.getPath());
                    if(sub != null && sub.length > 0){
                        out.add(f);
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return out;
    }

    public static void addTestsFromFile(String filename, List<String> list)
            throws IOException {
        addTestsFromStream(new FileInputStream(new File(filename)), list);
    }

    public static void addTestsFromStream(InputStream in, List<String> list)
            throws IOException {
        Properties props = new Properties();
        props.load(in);
        for (Object obj : props.keySet()) {
            list.add(obj.toString());
        }
    }

    public static String[] loadTestsFromResource(String resource, String[] inherited)
            throws IOException {
        List<String> list = inherited == null ?
                new ArrayList<String>() :
                new ArrayList<String>(Arrays.asList(inherited));
        InputStream in = null;
        try {
            in = InstrumentationRegistry.getContext().getAssets().open(resource);
        }catch (FileNotFoundException ignored){
        }
        if (in != null)
            addTestsFromStream(in, list);
        return list.toArray(new String[0]);
    }

    public static boolean matches(String[] patterns, String path) {
        for (int i = 0; i < patterns.length; i++) {
            if (path.startsWith(patterns[i])) {
                return true;
            }
        }
        return false;
    }

    public static final FileFilter JS_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getAbsolutePath().endsWith(".js");
        }
    };

    public static void addAssetLoading(ScriptableObject scriptableObject){
        scriptableObject.defineFunctionProperties(new String[]{"loadAsset"}, TestUtils.class, ScriptableObject.DONTENUM);
    }

    public static void loadAsset(Context cx, Scriptable thisObj,
                                 Object[] args, Function funObj)
    {
        for (Object arg : args) {
            String file = Context.toString(arg);
            Global.load(cx, thisObj, new Object[]{copyFromAssets(file)}, funObj);
        }
    }

    private static File copyFromAssets(String name) {
        FileOutputStream out = null;
        InputStream in = null;
        try {
            File file = new File(InstrumentationRegistry.getContext().getFilesDir(), name);
            new File(file.getParent()).mkdirs();
            out = new FileOutputStream(file);
            in = InstrumentationRegistry.getContext().getAssets().open(name);
            ByteStreams.copy(in, out);
            return file;
        } catch (IOException ignored) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

}
