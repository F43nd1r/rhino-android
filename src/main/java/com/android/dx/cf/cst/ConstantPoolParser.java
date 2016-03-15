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

package com.android.dx.cf.cst;

import com.android.dx.cf.iface.ParseException;
import com.android.dx.rop.cst.Constant;
import com.android.dx.rop.cst.CstDouble;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.rop.cst.CstFloat;
import com.android.dx.rop.cst.CstInteger;
import com.android.dx.rop.cst.CstInterfaceMethodRef;
import com.android.dx.rop.cst.CstLong;
import com.android.dx.rop.cst.CstMethodRef;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.rop.cst.StdConstantPool;
import com.android.dx.rop.type.Type;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;

import java.util.BitSet;

import static com.android.dx.cf.cst.ConstantTags.CONSTANT_Class;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_Double;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_Fieldref;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_Float;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_Integer;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_InterfaceMethodref;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_InvokeDynamic;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_Long;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_MethodHandle;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_MethodType;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_Methodref;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_NameAndType;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_String;
import static com.android.dx.cf.cst.ConstantTags.CONSTANT_Utf8;

/**
 * Parser for a constant pool embedded in a class file.
 */
public final class ConstantPoolParser {
    /** {@code non-null;} the bytes of the constant pool */
    private final ByteArray bytes;

    /** {@code non-null;} actual parsed constant pool contents */
    private final StdConstantPool pool;

    /** {@code non-null;} byte offsets to each cst */
    private final int[] offsets;

    /**
     * -1 || &gt;= 10; the end offset of this constant pool in the
     * {@code byte[]} which it came from or {@code -1} if not
     * yet parsed
     */
    private int endOffset;

    /**
     * Constructs an instance.
     *
     * @param bytes {@code non-null;} the bytes of the file
     */
    public ConstantPoolParser(ByteArray bytes) {
        int size = bytes.getUnsignedShort(8); // constant_pool_count

        this.bytes = bytes;
        this.pool = new StdConstantPool(size);
        this.offsets = new int[size];
        this.endOffset = -1;
    }

    /**
     * Gets the end offset of this constant pool in the {@code byte[]}
     * which it came from.
     *
     * @return {@code >= 10;} the end offset
     */
    public int getEndOffset() {
        parseIfNecessary();
        return endOffset;
    }

    /**
     * Gets the actual constant pool.
     *
     * @return {@code non-null;} the constant pool
     */
    public StdConstantPool getPool() {
        parseIfNecessary();
        return pool;
    }

    /**
     * Runs {@link #parse} if it has not yet been run successfully.
     */
    private void parseIfNecessary() {
        if (endOffset < 0) {
            parse();
        }
    }

    /**
     * Does the actual parsing.
     */
    private void parse() {
        determineOffsets();

        /*
         * Track the constant value's original string type. True if constants[i] was
         * a CONSTANT_Utf8, false for any other type including CONSTANT_string.
         */
        BitSet wasUtf8 = new BitSet(offsets.length);

        for (int i = 1; i < offsets.length; i++) {
            int offset = offsets[i];
            if ((offset != 0) && (pool.getOrNull(i) == null)) {
                parse0(i, wasUtf8);
            }
        }
    }

    /**
     * Populates {@link #offsets} and also completely parse utf8 constants.
     */
    private void determineOffsets() {
        int at = 10; // offset from the start of the file to the first cst
        int lastCategory;

        for (int i = 1; i < offsets.length; i += lastCategory) {
            offsets[i] = at;
            int tag = bytes.getUnsignedByte(at);
            try {
                switch (tag) {
                    case CONSTANT_Integer:
                    case CONSTANT_Float:
                    case CONSTANT_Fieldref:
                    case CONSTANT_Methodref:
                    case CONSTANT_InterfaceMethodref:
                    case CONSTANT_NameAndType: {
                        lastCategory = 1;
                        at += 5;
                        break;
                    }
                    case CONSTANT_Long:
                    case CONSTANT_Double: {
                        lastCategory = 2;
                        at += 9;
                        break;
                    }
                    case CONSTANT_Class:
                    case CONSTANT_String: {
                        lastCategory = 1;
                        at += 3;
                        break;
                    }
                    case CONSTANT_Utf8: {
                        lastCategory = 1;
                        at += bytes.getUnsignedShort(at + 1) + 3;
                        break;
                    }
                    case CONSTANT_MethodHandle: {
                        throw new ParseException("MethodHandle not supported");
                    }
                    case CONSTANT_MethodType: {
                        throw new ParseException("MethodType not supported");
                    }
                    case CONSTANT_InvokeDynamic: {
                        throw new ParseException("InvokeDynamic not supported");
                    }
                    default: {
                        throw new ParseException("unknown tag byte: " + Hex.u1(tag));
                    }
                }
            } catch (ParseException ex) {
                ex.addContext("...while preparsing cst " + Hex.u2(i) + " at offset " + Hex.u4(at));
                throw ex;
            }
        }

        endOffset = at;
    }

    /**
     * Parses the constant for the given index if it hasn't already been
     * parsed, also storing it in the constant pool. This will also
     * have the side effect of parsing any entries the indicated one
     * depends on.
     *
     * @param idx which constant
     * @return {@code non-null;} the parsed constant
     */
    private Constant parse0(int idx, BitSet wasUtf8) {
        Constant cst = pool.getOrNull(idx);
        if (cst != null) {
            return cst;
        }

        int at = offsets[idx];

        try {
            int tag = bytes.getUnsignedByte(at);
            switch (tag) {
                case CONSTANT_Utf8: {
                    cst = parseUtf8(at);
                    wasUtf8.set(idx);
                    break;
                }
                case CONSTANT_Integer: {
                    int value = bytes.getInt(at + 1);
                    cst = CstInteger.make(value);
                    break;
                }
                case CONSTANT_Float: {
                    int bits = bytes.getInt(at + 1);
                    cst = CstFloat.make(bits);
                    break;
                }
                case CONSTANT_Long: {
                    long value = bytes.getLong(at + 1);
                    cst = CstLong.make(value);
                    break;
                }
                case CONSTANT_Double: {
                    long bits = bytes.getLong(at + 1);
                    cst = CstDouble.make(bits);
                    break;
                }
                case CONSTANT_Class: {
                    int nameIndex = bytes.getUnsignedShort(at + 1);
                    CstString name = (CstString) parse0(nameIndex, wasUtf8);
                    cst = new CstType(Type.internClassName(name.getString()));
                    break;
                }
                case CONSTANT_String: {
                    int stringIndex = bytes.getUnsignedShort(at + 1);
                    cst = parse0(stringIndex, wasUtf8);
                    break;
                }
                case CONSTANT_Fieldref: {
                    int classIndex = bytes.getUnsignedShort(at + 1);
                    CstType type = (CstType) parse0(classIndex, wasUtf8);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = new CstFieldRef(type, nat);
                    break;
                }
                case CONSTANT_Methodref: {
                    int classIndex = bytes.getUnsignedShort(at + 1);
                    CstType type = (CstType) parse0(classIndex, wasUtf8);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = new CstMethodRef(type, nat);
                    break;
                }
                case CONSTANT_InterfaceMethodref: {
                    int classIndex = bytes.getUnsignedShort(at + 1);
                    CstType type = (CstType) parse0(classIndex, wasUtf8);
                    int natIndex = bytes.getUnsignedShort(at + 3);
                    CstNat nat = (CstNat) parse0(natIndex, wasUtf8);
                    cst = new CstInterfaceMethodRef(type, nat);
                    break;
                }
                case CONSTANT_NameAndType: {
                    int nameIndex = bytes.getUnsignedShort(at + 1);
                    CstString name = (CstString) parse0(nameIndex, wasUtf8);
                    int descriptorIndex = bytes.getUnsignedShort(at + 3);
                    CstString descriptor = (CstString) parse0(descriptorIndex, wasUtf8);
                    cst = new CstNat(name, descriptor);
                    break;
                }
                case CONSTANT_MethodHandle: {
                    throw new ParseException("MethodHandle not supported");
                }
                case CONSTANT_MethodType: {
                    throw new ParseException("MethodType not supported");
                }
                case CONSTANT_InvokeDynamic: {
                    throw new ParseException("InvokeDynamic not supported");
                }
                default: {
                    throw new ParseException("unknown tag byte: " + Hex.u1(tag));
                }
            }
        } catch (ParseException ex) {
            ex.addContext("...while parsing cst " + Hex.u2(idx) +
                          " at offset " + Hex.u4(at));
            throw ex;
        } catch (RuntimeException ex) {
            ParseException pe = new ParseException(ex);
            pe.addContext("...while parsing cst " + Hex.u2(idx) +
                          " at offset " + Hex.u4(at));
            throw pe;
        }

        pool.set(idx, cst);
        return cst;
    }

    /**
     * Parses a utf8 constant.
     *
     * @param at offset to the start of the constant (where the tag byte is)
     * @return {@code non-null;} the parsed value
     */
    private CstString parseUtf8(int at) {
        int length = bytes.getUnsignedShort(at + 1);

        at += 3; // Skip to the data.

        ByteArray ubytes = bytes.slice(at, at + length);

        try {
            return new CstString(ubytes);
        } catch (IllegalArgumentException ex) {
            // Translate the exception
            throw new ParseException(ex);
        }
    }
}
