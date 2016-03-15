/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dx.command.dexer;

import com.android.dex.DexFormat;
import com.android.dex.util.FileUtils;
import com.android.dx.Version;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.command.DxConsole;
import com.android.dx.command.UsageException;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.dex.file.EncodedMethod;
import com.android.dx.rop.annotation.Annotation;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Main class for the class file translator.
 */
public class Main {

    /**
     * {@code non-null;} the lengthy message that tries to discourage
     * people from defining core classes in applications
     */
    private static final String IN_RE_CORE_CLASSES =
            "Ill-advised or mistaken usage of a core class (java.* or javax.*)\n" +
                    "when not building a core library.\n\n" +
                    "This is often due to inadvertently including a core library file\n" +
                    "in your application's project, when using an IDE (such as\n" +
                    "Eclipse). If you are sure you're not intentionally defining a\n" +
                    "core class, then this is the most likely explanation of what's\n" +
                    "going on.\n\n" +
                    "However, you might actually be trying to define a class in a core\n" +
                    "namespace, the source of which you may have taken, for example,\n" +
                    "from a non-Android virtual machine project. This will most\n" +
                    "assuredly not work. At a minimum, it jeopardizes the\n" +
                    "compatibility of your app with future versions of the platform.\n" +
                    "It is also often of questionable legality.\n\n" +
                    "If you really intend to build a core library -- which is only\n" +
                    "appropriate as part of creating a full virtual machine\n" +
                    "distribution, as opposed to compiling an application -- then use\n" +
                    "the \"--core-library\" option to suppress this error message.\n\n" +
                    "If you go ahead and use \"--core-library\" but are in fact\n" +
                    "building an application, then be forewarned that your application\n" +
                    "will still fail to build or run, at some point. Please be\n" +
                    "prepared for angry customers who find, for example, that your\n" +
                    "application ceases to function once they upgrade their operating\n" +
                    "system. You will be to blame for this problem.\n\n" +
                    "If you are legitimately using some code that happens to be in a\n" +
                    "core package, then the easiest safe alternative you have is to\n" +
                    "repackage that code. That is, move the classes in question into\n" +
                    "your own package namespace. This means that they will never be in\n" +
                    "conflict with core system classes. JarJar is a tool that may help\n" +
                    "you in this endeavor. If you find that you cannot do this, then\n" +
                    "that is an indication that the path you are on will ultimately\n" +
                    "lead to pain, suffering, grief, and lamentation.\n";

    /**
     * {@code non-null;} name of the standard manifest file in {@code .jar}
     * files
     */
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    /**
     * {@code non-null;} attribute name for the (quasi-standard?)
     * {@code Created-By} attribute
     */
    private static final Attributes.Name CREATED_BY =
            new Attributes.Name("Created-By");

    /**
     * {@code non-null;} list of {@code javax} subpackages that are considered
     * to be "core". <b>Note:</b>: This list must be sorted, since it
     * is binary-searched.
     */
    private static final String[] JAVAX_CORE = {
            "accessibility", "crypto", "imageio", "management", "naming", "net",
            "print", "rmi", "security", "sip", "sound", "sql", "swing",
            "transaction", "xml"
    };

    /**
     * number of errors during processing
     */
    private static final AtomicInteger errors = new AtomicInteger(0);

    /**
     * {@code non-null;} parsed command-line arguments
     */
    private static Arguments args;

    /**
     * {@code non-null;} output file in-progress
     */
    private static DexFile outputDex;

    /**
     * {@code null-ok;} map of resources to include in the output, or
     * {@code null} if resources are being ignored
     */
    private static TreeMap<String, byte[]> outputResources;

    /**
     * Thread pool object used for multi-thread class translation.
     */
    private static ExecutorService classTranslatorPool;

    /**
     * Single thread executor, for collecting results of parallel translation,
     * and adding classes to dex file in original input file order.
     */
    private static ExecutorService classDefItemConsumer;

    /**
     * Futures for {@code classDefItemConsumer} tasks.
     */
    private static final List<Future<Boolean>> addToDexFutures =
            new ArrayList<>();

    /**
     * Lock object used to to coordinate dex file rotation, and
     * multi-threaded translation.
     */
    private static final Object dexRotationLock = new Object();

    /**
     * Record the number if method indices "reserved" for files
     * committed to translation in the context of the current dex
     * file, but not yet added.
     */
    private static int maxMethodIdsInProcess = 0;

    /**
     * Record the number if field indices "reserved" for files
     * committed to translation in the context of the current dex
     * file, but not yet added.
     */
    private static int maxFieldIdsInProcess = 0;

    /**
     * true if any files are successfully processed
     */
    private static volatile boolean anyFilesProcessed;

    private static final OutputStreamWriter humanOutWriter = null;

    /**
     * This class is uninstantiable.
     */
    private Main() {
        // This space intentionally left blank.
    }

    /**
     * {@code non-null;} Error message for too many method/field/type ids.
     */
    public static String getTooManyIdsErrorMessage() {
        return "You may try using " + Arguments.MULTI_DEX_OPTION + " option.";
    }

    private static void updateStatus(boolean res) {
        anyFilesProcessed |= res;
    }


    public static void dexClass(String outName, String name, byte[] bytes) throws IOException{
        //init args
        args = new Arguments();
        args.cfOptions = new CfOptions();
        args.dexOptions = new DexOptions();
        args.outName = outName;
        if (new File(outName).isDirectory()) {
            args.jarOutput = false;
        } else if (FileUtils.hasArchiveSuffix(outName)) {
            args.jarOutput = true;
        } else if (outName.endsWith(".dex") ||
                outName.equals("-")) {
            args.jarOutput = false;
        } else {
            System.err.println("unknown output extension: " +
                    outName);
            throw new UsageException();
        }
        //init file
        outputDex = new DexFile(args.dexOptions);
        //init pool
        classTranslatorPool = new ThreadPoolExecutor(args.numThreads,
                args.numThreads, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(2 * args.numThreads, true),
                new ThreadPoolExecutor.CallerRunsPolicy());
        //init consumer
        classDefItemConsumer = Executors.newSingleThreadExecutor();
        //init output map
        outputResources = new TreeMap<>();
        //do process
        processClass(name, bytes);
        try {
            classTranslatorPool.shutdown();
            classTranslatorPool.awaitTermination(600L, TimeUnit.SECONDS);
            classDefItemConsumer.shutdown();
            classDefItemConsumer.awaitTermination(600L, TimeUnit.SECONDS);

            for (Future<Boolean> f : addToDexFutures) {
                try {
                    f.get();
                } catch (ExecutionException ex) {
                    // Catch any previously uncaught exceptions from
                    // class translation and adding to dex.
                    int count = errors.incrementAndGet();
                    if (count < 10) {
                        DxConsole.err.println("Uncaught translation error: " + ex.getCause());
                    } else {
                        throw new InterruptedException("Too many errors");
                    }
                }
            }

        } catch (InterruptedException ie) {
            classTranslatorPool.shutdownNow();
            classDefItemConsumer.shutdownNow();
            throw new RuntimeException("Translation has been interrupted", ie);
        } catch (Exception e) {
            classTranslatorPool.shutdownNow();
            classDefItemConsumer.shutdownNow();
            e.printStackTrace(System.out);
            throw new RuntimeException("Unexpected exception in translator thread.", e);
        }
        //write to file
        byte[] outArray = null;

        if (!outputDex.isEmpty()) {
            outArray = writeDex(outputDex);
            if (outArray == null) {
                return;
            }
        }

        if (args.jarOutput) {
            // Effectively free up the (often massive) DexFile memory.
            outputDex = null;

            if (outArray != null) {
                outputResources.put(DexFormat.DEX_IN_JAR_NAME, outArray);
            }
            createJar(args.outName);
        } else if (outArray != null && args.outName != null) {
            OutputStream out = openOutput(args.outName);
            out.write(outArray);
            closeOutput(out);
        }
    }

    /**
     * Processes one classfile.
     *
     * @param name  {@code non-null;} name of the file, clipped such that it
     *              <i>should</i> correspond to the name of the class it contains
     * @param bytes {@code non-null;} contents of the file
     */
    private static void processClass(String name, byte[] bytes) {
        checkClassName(name);

        try {
            new DirectClassFileConsumer(null).call(
                    new ClassParserTask(name, bytes).call());
        } catch (Exception ex) {
            throw new RuntimeException("Exception parsing classes", ex);
        }

    }


    private static DirectClassFile parseClass(String name, byte[] bytes) {

        DirectClassFile cf = new DirectClassFile(bytes, name,
                args.cfOptions.strictNameCheck);
        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        cf.getMagic(); // triggers the actual parsing
        return cf;
    }

    private static ClassDefItem translateClass(DirectClassFile cf) {
        try {
            return CfTranslator.translate(cf, args.cfOptions,
                    args.dexOptions, outputDex);
        } catch (ParseException ex) {
            DxConsole.err.println("\ntrouble processing:");
            ex.printContext(DxConsole.err);
        }
        errors.incrementAndGet();
        return null;
    }

    private static void addClassToDex(ClassDefItem clazz) {
        synchronized (outputDex) {
            outputDex.add(clazz);
        }
    }

    /**
     * Check the class name to make sure it's not a "core library"
     * class. If there is a problem, this updates the error count and
     * throws an exception to stop processing.
     *
     * @param name {@code non-null;} the fully-qualified internal-form
     *             class name
     */
    private static void checkClassName(String name) {
        boolean bogus = false;

        if (name.startsWith("java/")) {
            bogus = true;
        } else if (name.startsWith("javax/")) {
            int slashAt = name.indexOf('/', 6);
            if (slashAt == -1) {
                // Top-level javax classes are verboten.
                bogus = true;
            } else {
                String pkg = name.substring(6, slashAt);
                bogus = (Arrays.binarySearch(JAVAX_CORE, pkg) >= 0);
            }
        }

        if (!bogus) {
            return;
        }

        /*
         * The user is probably trying to include an entire desktop
         * core library in a misguided attempt to get their application
         * working. Try to help them understand what's happening.
         */

        DxConsole.err.println("\ntrouble processing \"" + name + "\":\n\n" +
                IN_RE_CORE_CLASSES);
        errors.incrementAndGet();
        throw new StopProcessing();
    }

    /**
     * Converts {@link #outputDex} into a {@code byte[]} and do whatever
     * human-oriented dumping is required.
     *
     * @return {@code null-ok;} the converted {@code byte[]} or {@code null}
     * if there was a problem
     */
    private static byte[] writeDex(DexFile outputDex) {
        byte[] outArray = null;

        try {
            try {
                /*
                 * This is the usual case: Create an output .dex file,
                 * and write it, dump it, etc.
                 */
                outArray = outputDex.toDex(humanOutWriter, args.verboseDump);
            } finally {
            }
        } catch (Exception ex) {
            DxConsole.err.println("\ntrouble writing output: " +
                    ex.getMessage());
            return null;
        }
        return outArray;
    }

    /**
     * Creates a jar file from the resources (including dex file arrays).
     *
     * @param fileName {@code non-null;} name of the file
     * @return whether the creation was successful
     */
    private static void createJar(String fileName) {
        /*
         * Make or modify the manifest (as appropriate), put the dex
         * array into the resources map, and then process the entire
         * resources map in a uniform manner.
         */

        try {
            Manifest manifest = makeManifest();
            OutputStream out = openOutput(fileName);
            JarOutputStream jarOut = new JarOutputStream(out, manifest);

            try {
                for (Map.Entry<String, byte[]> e :
                        outputResources.entrySet()) {
                    String name = e.getKey();
                    byte[] contents = e.getValue();
                    JarEntry entry = new JarEntry(name);
                    int length = contents.length;

                    entry.setSize(length);
                    jarOut.putNextEntry(entry);
                    jarOut.write(contents);
                    jarOut.closeEntry();
                }
            } finally {
                jarOut.finish();
                jarOut.flush();
                closeOutput(out);
            }
        } catch (Exception ex) {
            DxConsole.err.println("\ntrouble writing output: " +
                    ex.getMessage());
        }

    }

    /**
     * Creates and returns the manifest to use for the output. This may
     * modify {@link #outputResources} (removing the pre-existing manifest).
     *
     * @return {@code non-null;} the manifest
     */
    private static Manifest makeManifest() throws IOException {
        byte[] manifestBytes = outputResources.get(MANIFEST_NAME);
        Manifest manifest;
        Attributes attribs;

        if (manifestBytes == null) {
            // We need to construct an entirely new manifest.
            manifest = new Manifest();
            attribs = manifest.getMainAttributes();
            attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        } else {
            manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
            attribs = manifest.getMainAttributes();
            outputResources.remove(MANIFEST_NAME);
        }

        String createdBy = attribs.getValue(CREATED_BY);
        if (createdBy == null) {
            createdBy = "";
        } else {
            createdBy += " + ";
        }
        createdBy += "dx " + Version.VERSION;

        attribs.put(CREATED_BY, createdBy);
        attribs.putValue("Dex-Location", DexFormat.DEX_IN_JAR_NAME);

        return manifest;
    }

    /**
     * Opens and returns the named file for writing, treating "-" specially.
     *
     * @param name {@code non-null;} the file name
     * @return {@code non-null;} the opened file
     */
    private static OutputStream openOutput(String name) throws IOException {
        if (name.equals("-") ||
                name.startsWith("-.")) {
            return System.out;
        }

        return new FileOutputStream(name);
    }

    /**
     * Flushes and closes the given output stream, except if it happens to be
     * {@link System#out} in which case this method does the flush but not
     * the close. This method will also silently do nothing if given a
     * {@code null} argument.
     *
     * @param stream {@code null-ok;} what to close
     */
    private static void closeOutput(OutputStream stream) throws IOException {
        if (stream == null) {
            return;
        }

        stream.flush();

        if (stream != System.out) {
            stream.close();
        }
    }

    /**
     * Dumps any method with the given name in the given file.
     *
     * @param dex    {@code non-null;} the dex file
     * @param fqName {@code non-null;} the fully-qualified name of the
     *               method(s)
     * @param out    {@code non-null;} where to dump to
     */
    private static void dumpMethod(DexFile dex, String fqName,
                                   OutputStreamWriter out) {
        boolean wildcard = fqName.endsWith("*");
        int lastDot = fqName.lastIndexOf('.');

        if ((lastDot <= 0) || (lastDot == (fqName.length() - 1))) {
            DxConsole.err.println("bogus fully-qualified method name: " +
                    fqName);
            return;
        }

        String className = fqName.substring(0, lastDot).replace('.', '/');
        String methodName = fqName.substring(lastDot + 1);
        ClassDefItem clazz = dex.getClassOrNull(className);

        if (clazz == null) {
            DxConsole.err.println("no such class: " + className);
            return;
        }

        if (wildcard) {
            methodName = methodName.substring(0, methodName.length() - 1);
        }

        ArrayList<EncodedMethod> allMeths = clazz.getMethods();
        TreeMap<CstNat, EncodedMethod> meths =
                new TreeMap<>();

        /*
         * Figure out which methods to include in the output, and get them
         * all sorted, so that the printout code is robust with respect to
         * changes in the underlying order.
         */
        for (EncodedMethod meth : allMeths) {
            String methName = meth.getName().getString();
            if ((wildcard && methName.startsWith(methodName)) ||
                    (!wildcard && methName.equals(methodName))) {
                meths.put(meth.getRef().getNat(), meth);
            }
        }

        if (meths.size() == 0) {
            DxConsole.err.println("no such method: " + fqName);
            return;
        }

        PrintWriter pw = new PrintWriter(out);

        for (EncodedMethod meth : meths.values()) {
            // TODO: Better stuff goes here, perhaps.
            meth.debugPrint(pw, args.verboseDump);

            /*
             * The (default) source file is an attribute of the class, but
             * it's useful to see it in method dumps.
             */
            CstString sourceFile = clazz.getSourceFile();
            if (sourceFile != null) {
                pw.println("  source file: " + sourceFile.toQuoted());
            }

            Annotations methodAnnotations =
                    clazz.getMethodAnnotations(meth.getRef());
            AnnotationsList parameterAnnotations =
                    clazz.getParameterAnnotations(meth.getRef());

            if (methodAnnotations != null) {
                pw.println("  method annotations:");
                for (Annotation a : methodAnnotations.getAnnotations()) {
                    pw.println("    " + a);
                }
            }

            if (parameterAnnotations != null) {
                pw.println("  parameter annotations:");
                int sz = parameterAnnotations.size();
                for (int i = 0; i < sz; i++) {
                    pw.println("    parameter " + i);
                    Annotations annotations = parameterAnnotations.get(i);
                    for (Annotation a : annotations.getAnnotations()) {
                        pw.println("      " + a);
                    }
                }
            }
        }

        pw.flush();
    }

    /**
     * Exception class used to halt processing prematurely.
     */
    private static class StopProcessing extends RuntimeException {
        // This space intentionally left blank.
    }

    /**
     * Command-line argument parser and access.
     */
    public static class Arguments {

        private static final String MAIN_DEX_LIST_OPTION = "--main-dex-list";

        private static final String MULTI_DEX_OPTION = "--multi-dex";

        /**
         * whether to run in debug mode
         */
        public final boolean debug = false;

        /**
         * whether to emit high-level verbose human-oriented output
         */
        public final boolean verbose = false;

        /**
         * whether to emit verbose human-oriented output in the dump file
         */
        public final boolean verboseDump = false;

        /**
         * {@code null-ok;} particular method to dump
         */
        public final String methodToDump = null;

        /**
         * {@code null-ok;} output file name for binary file
         */
        public String outName = null;

        /**
         * whether the binary output is to be a {@code .jar} file
         * instead of a plain {@code .dex}
         */
        public boolean jarOutput = false;

        /**
         * Options for class file transformation
         */
        public CfOptions cfOptions;

        /**
         * Options for dex file output
         */
        public DexOptions dexOptions;

        /**
         * number of threads to run with
         */
        public final int numThreads = 1;

        /**
         * generation of multiple dex is allowed
         */
        public final boolean multiDex = false;

    }

    /**
     * Callable helper class to parse class bytes.
     */
    private static class ClassParserTask implements Callable<DirectClassFile> {

        final String name;
        final byte[] bytes;

        private ClassParserTask(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        @Override
        public DirectClassFile call() throws Exception {

            return parseClass(name, bytes);
        }
    }

    /**
     * Callable helper class used to sequentially collect the results of
     * the (optionally parallel) translation phase, in correct input file order.
     * This class is also responsible for coordinating dex file rotation
     * with the ClassDefItemConsumer class.
     * We maintain invariant that the number of indices used in the current
     * dex file plus the max number of indices required by classes passed to
     * the translation phase and not yet added to the dex file, is less than
     * or equal to the dex file limit.
     * For each parsed file, we estimate the maximum number of indices it may
     * require. If passing the file to the translation phase would invalidate
     * the invariant, we wait, until the next class is added to the dex file,
     * and then reevaluate the invariant. If there are no further classes in
     * the translation phase, we rotate the dex file.
     */
    private static class DirectClassFileConsumer implements Callable<Void> {

        final Future<DirectClassFile> dcff;

        private DirectClassFileConsumer(Future<DirectClassFile> dcff) {
            this.dcff = dcff;
        }

        @Override
        public Void call() throws Exception {

            DirectClassFile cf = dcff.get();
            call(cf);
            return null;
        }

        private void call(DirectClassFile cf) {

            int maxMethodIdsInClass = 0;
            int maxFieldIdsInClass = 0;

            // Submit class to translation phase.
            Future<ClassDefItem> cdif = classTranslatorPool.submit(
                    new ClassTranslatorTask(cf));
            Future<Boolean> res = classDefItemConsumer.submit(new ClassDefItemConsumer(
                    cdif, maxMethodIdsInClass, maxFieldIdsInClass));
            addToDexFutures.add(res);
        }
    }


    /**
     * Callable helper class to translate classes in parallel
     */
    private static class ClassTranslatorTask implements Callable<ClassDefItem> {

        final DirectClassFile classFile;

        private ClassTranslatorTask(DirectClassFile classFile) {
            this.classFile = classFile;
        }

        @Override
        public ClassDefItem call() {
            return translateClass(classFile);
        }
    }

    /**
     * Callable helper class used to collect the results of
     * the parallel translation phase, adding the translated classes to
     * the current dex file in correct (deterministic) file order.
     * This class is also responsible for coordinating dex file rotation
     * with the DirectClassFileConsumer class.
     */
    private static class ClassDefItemConsumer implements Callable<Boolean> {

        final Future<ClassDefItem> futureClazz;
        final int maxMethodIdsInClass;
        final int maxFieldIdsInClass;

        private ClassDefItemConsumer(Future<ClassDefItem> futureClazz,
                                     int maxMethodIdsInClass, int maxFieldIdsInClass) {
            this.futureClazz = futureClazz;
            this.maxMethodIdsInClass = maxMethodIdsInClass;
            this.maxFieldIdsInClass = maxFieldIdsInClass;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                ClassDefItem clazz = futureClazz.get();
                if (clazz != null) {
                    addClassToDex(clazz);
                    updateStatus(true);
                }
                return true;
            } catch (ExecutionException ex) {
                // Rethrow previously uncaught translation exceptions.
                // These, as well as any exceptions from addClassToDex,
                // are handled and reported in processAllFiles().
                Throwable t = ex.getCause();
                throw (t instanceof Exception) ? (Exception) t : ex;
            } finally {
            }
        }
    }
}
