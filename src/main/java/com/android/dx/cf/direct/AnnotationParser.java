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

import com.android.dx.cf.iface.ParseException;
import com.android.dx.rop.annotation.Annotation;
import com.android.dx.rop.annotation.AnnotationVisibility;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.annotation.NameValuePair;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstAnnotation;
import com.android.dx.rop.cst.CstArray;
import com.android.dx.rop.cst.CstBoolean;
import com.android.dx.rop.cst.CstByte;
import com.android.dx.rop.cst.CstChar;
import com.android.dx.rop.cst.CstDouble;
import com.android.dx.rop.cst.CstEnumRef;
import com.android.dx.rop.cst.CstFloat;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstLong;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstShort;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.type.Type;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;
import java.io.IOException;

/**
 * Parser for annotations.
 */
final class AnnotationParser {

    /** {@code non-null;} constant pool to use */
    private final ConstantPool pool;

    /** {@code non-null;} input stream to parse from */
    private final ByteArray.MyDataInputStream input;

    /**
     * Constructs an instance.
     *
     * @param cf {@code non-null;} class file to parse from
     * @param offset {@code >= 0;} offset into the class file data to parse at
     * @param length {@code >= 0;} number of bytes left in the attribute data
     */
    public AnnotationParser(DirectClassFile cf, int offset, int length) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }

        this.pool = cf.getConstantPool();
        /* {@code non-null;} bytes of the attribute data */
        ByteArray bytes = cf.getBytes().slice(offset, offset + length);
        this.input = bytes.makeDataInputStream();
    }

    /**
     * Parses an annotation value ({@code element_value}) attribute.
     *
     * @return {@code non-null;} the parsed constant value
     */
    public Constant parseValueAttribute() {
        Constant result;

        try {
            result = parseValue();

            if (input.available() != 0) {
                throw new ParseException("extra data in attribute");
            }
        } catch (IOException ex) {
            // ByteArray.MyDataInputStream should never throw.
            throw new RuntimeException("shouldn't happen", ex);
        }

        return result;
    }

    /**
     * Parses a parameter annotation attribute.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotations
     * @return {@code non-null;} the parsed list of lists of annotations
     */
    public AnnotationsList parseParameterAttribute(
            AnnotationVisibility visibility) {
        AnnotationsList result;

        try {
            result = parseAnnotationsList(visibility);

            if (input.available() != 0) {
                throw new ParseException("extra data in attribute");
            }
        } catch (IOException ex) {
            // ByteArray.MyDataInputStream should never throw.
            throw new RuntimeException("shouldn't happen", ex);
        }

        return result;
    }

    /**
     * Parses an annotation attribute, per se.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotations
     * @return {@code non-null;} the list of annotations read from the attribute
     * data
     */
    public Annotations parseAnnotationAttribute(
            AnnotationVisibility visibility) {
        Annotations result;

        try {
            result = parseAnnotations(visibility);

            if (input.available() != 0) {
                throw new ParseException("extra data in attribute");
            }
        } catch (IOException ex) {
            // ByteArray.MyDataInputStream should never throw.
            throw new RuntimeException("shouldn't happen", ex);
        }

        return result;
    }

    /**
     * Parses a list of annotation lists.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotations
     * @return {@code non-null;} the list of annotation lists read from the attribute
     * data
     */
    private AnnotationsList parseAnnotationsList(
            AnnotationVisibility visibility) throws IOException {
        int count = input.readUnsignedByte();

        AnnotationsList outerList = new AnnotationsList(count);

        for (int i = 0; i < count; i++) {

            Annotations annotations = parseAnnotations(visibility);
            outerList.set(i, annotations);
        }

        outerList.setImmutable();
        return outerList;
    }

    /**
     * Parses an annotation list.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotations
     * @return {@code non-null;} the list of annotations read from the attribute
     * data
     */
    private Annotations parseAnnotations(AnnotationVisibility visibility)
            throws IOException {
        int count = input.readUnsignedShort();

        Annotations annotations = new Annotations();

        for (int i = 0; i < count; i++) {

            Annotation annotation = parseAnnotation(visibility);
            annotations.add(annotation);
        }

        annotations.setImmutable();
        return annotations;
    }

    /**
     * Parses a single annotation.
     *
     * @param visibility {@code non-null;} visibility of the parsed annotation
     * @return {@code non-null;} the parsed annotation
     */
    private Annotation parseAnnotation(AnnotationVisibility visibility)
            throws IOException {
        requireLength(4);

        int typeIndex = input.readUnsignedShort();
        int numElements = input.readUnsignedShort();
        CstString typeString = (CstString) pool.get(typeIndex);
        CstType type = new CstType(Type.intern(typeString.getString()));

        Annotation annotation = new Annotation(type, visibility);

        for (int i = 0; i < numElements; i++) {

            NameValuePair element = parseElement();
            annotation.add(element);
        }

        annotation.setImmutable();
        return annotation;
    }

    /**
     * Parses a {@link NameValuePair}.
     *
     * @return {@code non-null;} the parsed element
     */
    private NameValuePair parseElement() throws IOException {
        requireLength(5);

        int elementNameIndex = input.readUnsignedShort();
        CstString elementName = (CstString) pool.get(elementNameIndex);

        Constant value = parseValue();

        return new NameValuePair(elementName, value);
    }

    /**
     * Parses an annotation value.
     *
     * @return {@code non-null;} the parsed value
     */
    private Constant parseValue() throws IOException {
        int tag = input.readUnsignedByte();

        switch (tag) {
            case 'B': {
                CstInteger value = (CstInteger) parseConstant();
                return CstByte.make(value.getValue());
            }
            case 'C': {
                CstInteger value = (CstInteger) parseConstant();
                int intValue = value.getValue();
                return CstChar.make(value.getValue());
            }
            case 'D': {
                return parseConstant();
            }
            case 'F': {
                return parseConstant();
            }
            case 'I': {
                return parseConstant();
            }
            case 'J': {
                return parseConstant();
            }
            case 'S': {
                CstInteger value = (CstInteger) parseConstant();
                return CstShort.make(value.getValue());
            }
            case 'Z': {
                CstInteger value = (CstInteger) parseConstant();
                return CstBoolean.make(value.getValue());
            }
            case 'c': {
                int classInfoIndex = input.readUnsignedShort();
                CstString value = (CstString) pool.get(classInfoIndex);
                Type type = Type.internReturnType(value.getString());

                return new CstType(type);
            }
            case 's': {
                return parseConstant();
            }
            case 'e': {
                requireLength(4);

                int typeNameIndex = input.readUnsignedShort();
                int constNameIndex = input.readUnsignedShort();
                CstString typeName = (CstString) pool.get(typeNameIndex);
                CstString constName = (CstString) pool.get(constNameIndex);

                return new CstEnumRef(new CstNat(constName, typeName));
            }
            case '@': {
                Annotation annotation =
                    parseAnnotation(AnnotationVisibility.EMBEDDED);
                return new CstAnnotation(annotation);
            }
            case '[': {
                requireLength(2);

                int numValues = input.readUnsignedShort();
                CstArray.List list = new CstArray.List(numValues);

                for (int i = 0; i < numValues; i++) {
                    list.set(i, parseValue());
                }

                list.setImmutable();
                return new CstArray(list);
            }
            default: {
                throw new ParseException("unknown annotation tag: " +
                        Hex.u1(tag));
            }
        }
    }

    /**
     * Helper for {@link #parseValue}, which parses a constant reference
     * and returns the referred-to constant value.
     *
     * @return {@code non-null;} the parsed value
     */
    private Constant parseConstant() throws IOException {
        int constValueIndex = input.readUnsignedShort();

        return pool.get(constValueIndex);
    }

    /**
     * Helper which will throw an exception if the given number of bytes
     * is not available to be read.
     *
     * @param requiredLength the number of required bytes
     */
    private void requireLength(int requiredLength) throws IOException {
        if (input.available() < requiredLength) {
            throw new ParseException("truncated annotation attribute");
        }
    }

}
