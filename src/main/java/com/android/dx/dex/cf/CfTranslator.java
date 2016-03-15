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

package com.android.dx.dex.cf;

import com.android.dex.util.ExceptionWithContext;
import com.android.dx.cf.code.ConcreteMethod;
import com.android.dx.cf.code.Ropper;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.iface.Field;
import com.android.dx.cf.iface.FieldList;
import com.android.dx.cf.iface.Method;
import com.android.dx.cf.iface.MethodList;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.code.DalvCode;
import com.android.dx.dex.code.RopTranslator;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.dex.file.EncodedField;
import com.android.dx.dex.file.EncodedMethod;
import com.android.dx.dex.file.FieldIdsSection;
import com.android.dx.dex.file.MethodIdsSection;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.code.DexTranslationAdvice;
import com.android.dx.rop.code.LocalVariableInfo;
import com.android.dx.rop.code.RopMethod;
import com.android.dx.rop.code.TranslationAdvice;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstBaseMethodRef;
import com.android.dx.rop.cst.CstBoolean;
import com.android.dx.rop.cst.CstByte;
import com.android.dx.rop.cst.CstChar;
import com.android.dx.rop.cst.CstEnumRef;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstInterfaceMethodRef;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstShort;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.TypedConstant;
import com.android.dx.rop.type.Type;
import com.android.dx.rop.type.TypeList;

/**
 * Static method that turns {@code byte[]}s containing Java
 * classfiles into {@link ClassDefItem} instances.
 */
public class CfTranslator {
    /** set to {@code true} to enable development-time debugging code */
    private static final boolean DEBUG = false;

    /**
     * This class is uninstantiable.
     */
    private CfTranslator() {
        // This space intentionally left blank.
    }

    /**
     * Takes a {@code byte[]}, interprets it as a Java classfile, and
     * translates it into a {@link ClassDefItem}.
     *
     * @param cf {@code non-null;} the class file
     * @param cfOptions options for class translation
     * @param dexOptions options for dex output
     * @param dexFile {@code non-null;} dex output
     * @return {@code non-null;} the translated class
     */
    public static ClassDefItem translate(DirectClassFile cf,
                                         CfOptions cfOptions, DexOptions dexOptions, DexFile dexFile) {
        try {
            return translate0(cf, cfOptions, dexOptions, dexFile);
        } catch (RuntimeException ex) {
            String msg = "...while processing " + cf.getFilePath();
            throw ExceptionWithContext.withContext(ex, msg);
        }
    }

    /**
     * Performs the main act of translation. This method is separated
     * from {@link #translate} just to keep things a bit simpler in
     * terms of exception handling.
     *
     * @param cf {@code non-null;} the class file
     * @param cfOptions options for class translation
     * @param dexOptions options for dex output
     * @param dexFile {@code non-null;} dex output
     * @return {@code non-null;} the translated class
     */
    private static ClassDefItem translate0(DirectClassFile cf,
                                           CfOptions cfOptions, DexOptions dexOptions, DexFile dexFile) {

        OptimizerOptions.loadOptimizeLists(cfOptions.optimizeListFile,
                cfOptions.dontOptimizeListFile);

        // Build up a class to output.

        CstType thisClass = cf.getThisClass();
        int classAccessFlags = cf.getAccessFlags() & ~AccessFlags.ACC_SUPER;
        CstString sourceFile = cf.getSourceFile();
        ClassDefItem out =
            new ClassDefItem(thisClass, classAccessFlags,
                    cf.getSuperclass(), cf.getInterfaces(), sourceFile);

        Annotations classAnnotations =
            AttributeTranslator.getClassAnnotations(cf, cfOptions);
        if (classAnnotations.size() != 0) {
            out.setClassAnnotations(classAnnotations, dexFile);
        }

        FieldIdsSection fieldIdsSection = dexFile.getFieldIds();
        MethodIdsSection methodIdsSection = dexFile.getMethodIds();
        processFields(cf, out, dexFile);
        processMethods(cf, cfOptions, dexOptions, out, dexFile);

        // intern constant pool method, field and type references
        ConstantPool constantPool = cf.getConstantPool();
        int constantPoolSize = constantPool.size();

        for (int i = 0; i < constantPoolSize; i++) {
            Constant constant = constantPool.getOrNull(i);
            if (constant instanceof CstMethodRef) {
                methodIdsSection.intern((CstBaseMethodRef) constant);
            } else if (constant instanceof CstInterfaceMethodRef) {
                methodIdsSection.intern(((CstInterfaceMethodRef) constant).toMethodRef());
            } else if (constant instanceof CstFieldRef) {
                fieldIdsSection.intern((CstFieldRef) constant);
            } else if (constant instanceof CstEnumRef) {
                fieldIdsSection.intern(((CstEnumRef) constant).getFieldRef());
            }
        }

        return out;
    }

    /**
     * Processes the fields of the given class.
     *
     * @param cf {@code non-null;} class being translated
     * @param out {@code non-null;} output class
     * @param dexFile {@code non-null;} dex output
     */
    private static void processFields(
            DirectClassFile cf, ClassDefItem out, DexFile dexFile) {
        CstType thisClass = cf.getThisClass();
        FieldList fields = cf.getFields();
        int sz = fields.size();

        for (int i = 0; i < sz; i++) {
            Field one = fields.get(i);
            try {
                CstFieldRef field = new CstFieldRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();

                if (AccessFlags.isStatic(accessFlags)) {
                    TypedConstant constVal = one.getConstantValue();
                    EncodedField fi = new EncodedField(field, accessFlags);
                    if (constVal != null) {
                        constVal = coerceConstant(constVal, field.getType());
                    }
                    out.addStaticField(fi, constVal);
                } else {
                    EncodedField fi = new EncodedField(field, accessFlags);
                    out.addInstanceField(fi);
                }

                Annotations annotations =
                    AttributeTranslator.getAnnotations(one.getAttributes());
                if (annotations.size() != 0) {
                    out.addFieldAnnotations(field, annotations, dexFile);
                }
                dexFile.getFieldIds().intern(field);
            } catch (RuntimeException ex) {
                String msg = "...while processing " + one.getName().toHuman() +
                    " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }

    /**
     * Helper for {@link #processFields}, which translates constants into
     * more specific types if necessary.
     *
     * @param constant {@code non-null;} the constant in question
     * @param type {@code non-null;} the desired type
     */
    private static TypedConstant coerceConstant(TypedConstant constant,
            Type type) {
        Type constantType = constant.getType();

        if (constantType.equals(type)) {
            return constant;
        }

        switch (type.getBasicType()) {
            case Type.BT_BOOLEAN: {
                return CstBoolean.make(((CstInteger) constant).getValue());
            }
            case Type.BT_BYTE: {
                return CstByte.make(((CstInteger) constant).getValue());
            }
            case Type.BT_CHAR: {
                return CstChar.make(((CstInteger) constant).getValue());
            }
            case Type.BT_SHORT: {
                return CstShort.make(((CstInteger) constant).getValue());
            }
            default: {
                throw new UnsupportedOperationException("can't coerce " +
                        constant + " to " + type);
            }
        }
    }

    /**
     * Processes the methods of the given class.
     *
     * @param cf {@code non-null;} class being translated
     * @param cfOptions {@code non-null;} options for class translation
     * @param dexOptions {@code non-null;} options for dex output
     * @param out {@code non-null;} output class
     * @param dexFile {@code non-null;} dex output
     */
    private static void processMethods(DirectClassFile cf, CfOptions cfOptions,
            DexOptions dexOptions, ClassDefItem out, DexFile dexFile) {
        CstType thisClass = cf.getThisClass();
        MethodList methods = cf.getMethods();
        int sz = methods.size();

        for (int i = 0; i < sz; i++) {
            Method one = methods.get(i);
            try {
                CstMethodRef meth = new CstMethodRef(thisClass, one.getNat());
                int accessFlags = one.getAccessFlags();
                boolean isStatic = AccessFlags.isStatic(accessFlags);
                boolean isPrivate = AccessFlags.isPrivate(accessFlags);
                boolean isNative = AccessFlags.isNative(accessFlags);
                boolean isAbstract = AccessFlags.isAbstract(accessFlags);
                boolean isConstructor = meth.isInstanceInit() ||
                    meth.isClassInit();
                DalvCode code;

                if (isNative || isAbstract) {
                    // There's no code for native or abstract methods.
                    code = null;
                } else {
                    ConcreteMethod concrete =
                        new ConcreteMethod(one, cf,
                                true,
                                cfOptions.localInfo);

                    TranslationAdvice advice;

                    advice = DexTranslationAdvice.THE_ONE;

                    RopMethod rmeth = Ropper.convert(concrete, advice, methods);
                    RopMethod nonOptRmeth = null;
                    int paramSize;

                    paramSize = meth.getParameterWordCount(isStatic);

                    String canonicalName
                            = thisClass.getClassType().getDescriptor()
                                + "." + one.getName().getString();

                    LocalVariableInfo locals = null;

                    code = RopTranslator.translate(rmeth, cfOptions.positionInfo,
                            null, paramSize, dexOptions);
                }

                // Preserve the synchronized flag as its "declared" variant...
                if (AccessFlags.isSynchronized(accessFlags)) {
                    accessFlags |= AccessFlags.ACC_DECLARED_SYNCHRONIZED;

                    /*
                     * ...but only native methods are actually allowed to be
                     * synchronized.
                     */
                    if (!isNative) {
                        accessFlags &= ~AccessFlags.ACC_SYNCHRONIZED;
                    }
                }

                if (isConstructor) {
                    accessFlags |= AccessFlags.ACC_CONSTRUCTOR;
                }

                TypeList exceptions = AttributeTranslator.getExceptions(one);
                EncodedMethod mi =
                    new EncodedMethod(meth, accessFlags, code, exceptions);

                if (meth.isInstanceInit() || meth.isClassInit() ||
                    isStatic || isPrivate) {
                    out.addDirectMethod(mi);
                } else {
                    out.addVirtualMethod(mi);
                }

                Annotations annotations =
                    AttributeTranslator.getMethodAnnotations(one);
                if (annotations.size() != 0) {
                    out.addMethodAnnotations(meth, annotations, dexFile);
                }

                AnnotationsList list =
                    AttributeTranslator.getParameterAnnotations(one);
                if (list.size() != 0) {
                    out.addParameterAnnotations(meth, list, dexFile);
                }
                dexFile.getMethodIds().intern(meth);
            } catch (RuntimeException ex) {
                String msg = "...while processing " + one.getName().toHuman() +
                    " " + one.getDescriptor().toHuman();
                throw ExceptionWithContext.withContext(ex, msg);
            }
        }
    }

}
