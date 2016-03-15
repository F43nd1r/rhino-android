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

package com.android.dx.cf.direct;

import com.android.dx.cf.attrib.AttAnnotationDefault;
import com.android.dx.cf.attrib.AttCode;
import com.android.dx.cf.attrib.AttConstantValue;
import com.android.dx.cf.attrib.AttDeprecated;
import com.android.dx.cf.attrib.AttEnclosingMethod;
import com.android.dx.cf.attrib.AttExceptions;
import com.android.dx.cf.attrib.AttInnerClasses;
import com.android.dx.cf.attrib.AttLineNumberTable;
import com.android.dx.cf.attrib.AttLocalVariableTable;
import com.android.dx.cf.attrib.AttLocalVariableTypeTable;
import com.android.dx.cf.attrib.AttRuntimeInvisibleAnnotations;
import com.android.dx.cf.attrib.AttRuntimeInvisibleParameterAnnotations;
import com.android.dx.cf.attrib.AttRuntimeVisibleAnnotations;
import com.android.dx.cf.attrib.AttRuntimeVisibleParameterAnnotations;
import com.android.dx.cf.attrib.AttSignature;
import com.android.dx.cf.attrib.AttSourceFile;
import com.android.dx.cf.attrib.AttSynthetic;
import com.android.dx.cf.attrib.InnerClassList;
import com.android.dx.cf.code.ByteCatchList;
import com.android.dx.cf.code.BytecodeArray;
import com.android.dx.cf.code.LineNumberList;
import com.android.dx.cf.code.LocalVariableList;
import com.android.dx.cf.iface.Attribute;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.cf.iface.StdAttributeList;
import com.android.dx.rop.annotation.AnnotationVisibility;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.TypedConstant;
import com.android.dx.rop.type.TypeList;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;

import java.io.IOException;

/**
 * Standard subclass of {@link AttributeFactory}, which knows how to parse
 * all the standard attribute types.
 */
public class StdAttributeFactory
    extends AttributeFactory {
    /** {@code non-null;} shared instance of this class */
    public static final StdAttributeFactory THE_ONE =
        new StdAttributeFactory();

    /**
     * Constructs an instance.
     */
    private StdAttributeFactory() {
        // This space intentionally left blank.
    }

    /** {@inheritDoc} */
    @Override
    protected Attribute parse0(DirectClassFile cf, int context, String name,
            int offset, int length) {
        switch (context) {
            case CTX_CLASS: {
                if (name == AttDeprecated.ATTRIBUTE_NAME) {
                    return deprecated(length);
                }
                if (name == AttEnclosingMethod.ATTRIBUTE_NAME) {
                    return enclosingMethod(cf, offset, length);
                }
                if (name == AttInnerClasses.ATTRIBUTE_NAME) {
                    return innerClasses(cf, offset, length);
                }
                if (name == AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeInvisibleAnnotations(cf, offset, length);
                }
                if (name == AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeVisibleAnnotations(cf, offset, length);
                }
                if (name == AttSynthetic.ATTRIBUTE_NAME) {
                    return synthetic(length);
                }
                if (name == AttSignature.ATTRIBUTE_NAME) {
                    return signature(cf, offset, length);
                }
                if (name == AttSourceFile.ATTRIBUTE_NAME) {
                    return sourceFile(cf, offset, length);
                }
                break;
            }
            case CTX_FIELD: {
                if (name == AttConstantValue.ATTRIBUTE_NAME) {
                    return constantValue(cf, offset, length);
                }
                if (name == AttDeprecated.ATTRIBUTE_NAME) {
                    return deprecated(length);
                }
                if (name == AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeInvisibleAnnotations(cf, offset, length);
                }
                if (name == AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeVisibleAnnotations(cf, offset, length);
                }
                if (name == AttSignature.ATTRIBUTE_NAME) {
                    return signature(cf, offset, length);
                }
                if (name == AttSynthetic.ATTRIBUTE_NAME) {
                    return synthetic(length);
                }
                break;
            }
            case CTX_METHOD: {
                if (name == AttAnnotationDefault.ATTRIBUTE_NAME) {
                    return annotationDefault(cf, offset, length);
                }
                if (name == AttCode.ATTRIBUTE_NAME) {
                    return code(cf, offset, length);
                }
                if (name == AttDeprecated.ATTRIBUTE_NAME) {
                    return deprecated(length);
                }
                if (name == AttExceptions.ATTRIBUTE_NAME) {
                    return exceptions(cf, offset, length);
                }
                if (name == AttRuntimeInvisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeInvisibleAnnotations(cf, offset, length);
                }
                if (name == AttRuntimeVisibleAnnotations.ATTRIBUTE_NAME) {
                    return runtimeVisibleAnnotations(cf, offset, length);
                }
                if (name == AttRuntimeInvisibleParameterAnnotations.
                        ATTRIBUTE_NAME) {
                    return runtimeInvisibleParameterAnnotations(
                            cf, offset, length);
                }
                if (name == AttRuntimeVisibleParameterAnnotations.
                        ATTRIBUTE_NAME) {
                    return runtimeVisibleParameterAnnotations(
                            cf, offset, length);
                }
                if (name == AttSignature.ATTRIBUTE_NAME) {
                    return signature(cf, offset, length);
                }
                if (name == AttSynthetic.ATTRIBUTE_NAME) {
                    return synthetic(length);
                }
                break;
            }
            case CTX_CODE: {
                if (name == AttLineNumberTable.ATTRIBUTE_NAME) {
                    return lineNumberTable(cf, offset, length);
                }
                if (name == AttLocalVariableTable.ATTRIBUTE_NAME) {
                    return localVariableTable(cf, offset, length);
                }
                if (name == AttLocalVariableTypeTable.ATTRIBUTE_NAME) {
                    return localVariableTypeTable(cf, offset, length);
                }
                break;
            }
        }

        return super.parse0(cf, context, name, offset, length);
    }

    /**
     * Parses an {@code AnnotationDefault} attribute.
     */
    private Attribute annotationDefault(DirectClassFile cf,
            int offset, int length) {
        if (length < 2) {
            throwSeverelyTruncated();
        }

        AnnotationParser ap =
            new AnnotationParser(cf, offset, length);
        Constant cst = ap.parseValueAttribute();

        return new AttAnnotationDefault(cst, length);
    }

    /**
     * Parses a {@code Code} attribute.
     */
    private Attribute code(DirectClassFile cf, int offset, int length) {
        if (length < 12) {
            return throwSeverelyTruncated();
        }

        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();
        int maxStack = bytes.getUnsignedShort(offset); // u2 max_stack
        int maxLocals = bytes.getUnsignedShort(offset + 2); // u2 max_locals
        int codeLength = bytes.getInt(offset + 4); // u4 code_length
        int origOffset = offset;

        offset += 8;
        length -= 8;

        if (length < (codeLength + 4)) {
            return throwTruncated();
        }

        int codeOffset = offset;
        offset += codeLength;
        length -= codeLength;
        BytecodeArray code =
            new BytecodeArray(bytes.slice(codeOffset, codeOffset + codeLength),
                              pool);

        // u2 exception_table_length
        int exceptionTableLength = bytes.getUnsignedShort(offset);
        ByteCatchList catches = (exceptionTableLength == 0) ?
            ByteCatchList.EMPTY :
            new ByteCatchList(exceptionTableLength);

        offset += 2;
        length -= 2;

        if (length < (exceptionTableLength * 8 + 2)) {
            return throwTruncated();
        }

        for (int i = 0; i < exceptionTableLength; i++) {

            int startPc = bytes.getUnsignedShort(offset);
            int endPc = bytes.getUnsignedShort(offset + 2);
            int handlerPc = bytes.getUnsignedShort(offset + 4);
            int catchTypeIdx = bytes.getUnsignedShort(offset + 6);
            CstType catchType = (CstType) pool.get0Ok(catchTypeIdx);
            catches.set(i, startPc, endPc, handlerPc, catchType);
            offset += 8;
            length -= 8;
        }

        catches.setImmutable();

        AttributeListParser parser =
            new AttributeListParser(cf, CTX_CODE, offset, this);

        StdAttributeList attributes = parser.getList();
        attributes.setImmutable();

        int attributeByteCount = parser.getEndOffset() - offset;
        if (attributeByteCount != length) {
            return throwBadLength(attributeByteCount + (offset - origOffset));
        }

        return new AttCode(maxStack, maxLocals, code, catches, attributes);
    }

    /**
     * Parses a {@code ConstantValue} attribute.
     */
    private Attribute constantValue(DirectClassFile cf, int offset, int length) {
        if (length != 2) {
            return throwBadLength(2);
        }

        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();
        int idx = bytes.getUnsignedShort(offset);
        TypedConstant cst = (TypedConstant) pool.get(idx);

        return new AttConstantValue(cst);
    }

    /**
     * Parses a {@code Deprecated} attribute.
     */
    private Attribute deprecated(int length) {
        if (length != 0) {
            return throwBadLength(0);
        }

        return new AttDeprecated();
    }

    /**
     * Parses an {@code EnclosingMethod} attribute.
     */
    private Attribute enclosingMethod(DirectClassFile cf, int offset,
            int length) {
        if (length != 4) {
            throwBadLength(4);
        }

        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();

        int idx = bytes.getUnsignedShort(offset);
        CstType type = (CstType) pool.get(idx);

        idx = bytes.getUnsignedShort(offset + 2);
        CstNat method = (CstNat) pool.get0Ok(idx);

        return new AttEnclosingMethod(type, method);
    }

    /**
     * Parses an {@code Exceptions} attribute.
     */
    private Attribute exceptions(DirectClassFile cf, int offset, int length) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }

        ByteArray bytes = cf.getBytes();
        int count = bytes.getUnsignedShort(offset); // number_of_exceptions

        offset += 2;
        length -= 2;

        if (length != (count * 2)) {
            throwBadLength((count * 2) + 2);
        }

        TypeList list = cf.makeTypeList(offset, count);
        return new AttExceptions(list);
    }

    /**
     * Parses an {@code InnerClasses} attribute.
     */
    private Attribute innerClasses(DirectClassFile cf, int offset, int length) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }

        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();
        int count = bytes.getUnsignedShort(offset); // number_of_classes

        offset += 2;
        length -= 2;

        if (length != (count * 8)) {
            throwBadLength((count * 8) + 2);
        }

        InnerClassList list = new InnerClassList(count);

        for (int i = 0; i < count; i++) {
            int innerClassIdx = bytes.getUnsignedShort(offset);
            int outerClassIdx = bytes.getUnsignedShort(offset + 2);
            int nameIdx = bytes.getUnsignedShort(offset + 4);
            int accessFlags = bytes.getUnsignedShort(offset + 6);
            CstType innerClass = (CstType) pool.get(innerClassIdx);
            CstType outerClass = (CstType) pool.get0Ok(outerClassIdx);
            CstString name = (CstString) pool.get0Ok(nameIdx);
            list.set(i, innerClass, outerClass, name, accessFlags);
            offset += 8;
        }

        list.setImmutable();
        return new AttInnerClasses(list);
    }

    /**
     * Parses a {@code LineNumberTable} attribute.
     */
    private Attribute lineNumberTable(DirectClassFile cf, int offset,
            int length) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }

        ByteArray bytes = cf.getBytes();
        int count = bytes.getUnsignedShort(offset); // line_number_table_length

        offset += 2;
        length -= 2;

        if (length != (count * 4)) {
            throwBadLength((count * 4) + 2);
        }

        LineNumberList list = new LineNumberList(count);

        for (int i = 0; i < count; i++) {
            int startPc = bytes.getUnsignedShort(offset);
            int lineNumber = bytes.getUnsignedShort(offset + 2);
            list.set(i, startPc, lineNumber);
            offset += 4;
        }

        list.setImmutable();
        return new AttLineNumberTable(list);
    }

    /**
     * Parses a {@code LocalVariableTable} attribute.
     */
    private Attribute localVariableTable(DirectClassFile cf, int offset,
            int length) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }

        ByteArray bytes = cf.getBytes();
        int count = bytes.getUnsignedShort(offset);
        LocalVariableList list = parseLocalVariables(
                bytes.slice(offset + 2, offset + length), cf.getConstantPool(),
                count, false);
        return new AttLocalVariableTable(list);
    }

    /**
     * Parses a {@code LocalVariableTypeTable} attribute.
     */
    private Attribute localVariableTypeTable(DirectClassFile cf, int offset,
            int length) {
        if (length < 2) {
            return throwSeverelyTruncated();
        }

        ByteArray bytes = cf.getBytes();
        int count = bytes.getUnsignedShort(offset);

        LocalVariableList list = parseLocalVariables(
                bytes.slice(offset + 2, offset + length), cf.getConstantPool(),
                count, true);
        return new AttLocalVariableTypeTable(list);
    }

    /**
     * Parse the table part of either a {@code LocalVariableTable}
     * or a {@code LocalVariableTypeTable}.
     *
     * @param bytes {@code non-null;} bytes to parse, which should <i>only</i>
     * contain the table data (no header)
     * @param pool {@code non-null;} constant pool to use
     * @param count {@code >= 0;} the number of entries
     * @param typeTable {@code true} iff this is for a type table
     * @return {@code non-null;} the constructed list
     */
    private LocalVariableList parseLocalVariables(ByteArray bytes,
            ConstantPool pool,  int count,
            boolean typeTable) {
        if (bytes.size() != (count * 10)) {
            // "+ 2" is for the count.
            throwBadLength((count * 10) + 2);
        }

        ByteArray.MyDataInputStream in = bytes.makeDataInputStream();
        LocalVariableList list = new LocalVariableList(count);

        try {
            for (int i = 0; i < count; i++) {
                int startPc = in.readUnsignedShort();
                int length = in.readUnsignedShort();
                int nameIdx = in.readUnsignedShort();
                int typeIdx = in.readUnsignedShort();
                int index = in.readUnsignedShort();
                CstString name = (CstString) pool.get(nameIdx);
                CstString type = (CstString) pool.get(typeIdx);
                CstString descriptor = null;
                CstString signature = null;

                if (typeTable) {
                    signature = type;
                } else {
                    descriptor = type;
                }

                list.set(i, startPc, length, name,
                        descriptor, signature, index);
            }
        } catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }

        list.setImmutable();
        return list;
    }

    /**
     * Parses a {@code RuntimeInvisibleAnnotations} attribute.
     */
    private Attribute runtimeInvisibleAnnotations(DirectClassFile cf,
            int offset, int length) {
        if (length < 2) {
            throwSeverelyTruncated();
        }

        AnnotationParser ap =
            new AnnotationParser(cf, offset, length);
        Annotations annotations =
            ap.parseAnnotationAttribute(AnnotationVisibility.BUILD);

        return new AttRuntimeInvisibleAnnotations(annotations, length);
    }

    /**
     * Parses a {@code RuntimeVisibleAnnotations} attribute.
     */
    private Attribute runtimeVisibleAnnotations(DirectClassFile cf,
            int offset, int length) {
        if (length < 2) {
            throwSeverelyTruncated();
        }

        AnnotationParser ap =
            new AnnotationParser(cf, offset, length);
        Annotations annotations =
            ap.parseAnnotationAttribute(AnnotationVisibility.RUNTIME);

        return new AttRuntimeVisibleAnnotations(annotations, length);
    }

    /**
     * Parses a {@code RuntimeInvisibleParameterAnnotations} attribute.
     */
    private Attribute runtimeInvisibleParameterAnnotations(DirectClassFile cf,
            int offset, int length) {
        if (length < 2) {
            throwSeverelyTruncated();
        }

        AnnotationParser ap =
            new AnnotationParser(cf, offset, length);
        AnnotationsList list =
            ap.parseParameterAttribute(AnnotationVisibility.BUILD);

        return new AttRuntimeInvisibleParameterAnnotations(list, length);
    }

    /**
     * Parses a {@code RuntimeVisibleParameterAnnotations} attribute.
     */
    private Attribute runtimeVisibleParameterAnnotations(DirectClassFile cf,
            int offset, int length) {
        if (length < 2) {
            throwSeverelyTruncated();
        }

        AnnotationParser ap =
            new AnnotationParser(cf, offset, length);
        AnnotationsList list =
            ap.parseParameterAttribute(AnnotationVisibility.RUNTIME);

        return new AttRuntimeVisibleParameterAnnotations(list, length);
    }

    /**
     * Parses a {@code Signature} attribute.
     */
    private Attribute signature(DirectClassFile cf, int offset, int length) {
        if (length != 2) {
            throwBadLength(2);
        }

        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();
        int idx = bytes.getUnsignedShort(offset);
        CstString cst = (CstString) pool.get(idx);

        return new AttSignature(cst);
    }

    /**
     * Parses a {@code SourceFile} attribute.
     */
    private Attribute sourceFile(DirectClassFile cf, int offset, int length) {
        if (length != 2) {
            throwBadLength(2);
        }

        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();
        int idx = bytes.getUnsignedShort(offset);
        CstString cst = (CstString) pool.get(idx);

        return new AttSourceFile(cst);
    }

    /**
     * Parses a {@code Synthetic} attribute.
     */
    private Attribute synthetic(int length) {
        if (length != 0) {
            return throwBadLength(0);
        }

        return new AttSynthetic();
    }

    /**
     * Throws the right exception when a known attribute has a way too short
     * length.
     *
     * @return never
     * @throws ParseException always thrown
     */
    private static Attribute throwSeverelyTruncated() {
        throw new ParseException("severely truncated attribute");
    }

    /**
     * Throws the right exception when a known attribute has a too short
     * length.
     *
     * @return never
     * @throws ParseException always thrown
     */
    private static Attribute throwTruncated() {
        throw new ParseException("truncated attribute");
    }

    /**
     * Throws the right exception when an attribute has an unexpected length
     * (given its contents).
     *
     * @param expected expected length
     * @return never
     * @throws ParseException always thrown
     */
    private static Attribute throwBadLength(int expected) {
        throw new ParseException("bad attribute length; expected length " +
                                 Hex.u4(expected));
    }
}
