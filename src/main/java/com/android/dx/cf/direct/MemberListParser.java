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

import com.android.dx.cf.iface.AttributeList;
import com.android.dx.cf.iface.Member;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.cf.iface.StdAttributeList;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;
import com.android.dx.rop.cst.CstType;
import com.android.dx.util.ByteArray;

/**
 * Parser for lists of class file members (that is, fields and methods).
 */
abstract /*package*/ class MemberListParser {
    /** {@code non-null;} the class file to parse from */
    private final DirectClassFile cf;

    /** {@code non-null;} class being defined */
    private final CstType definer;

    /** offset in the byte array of the classfile to the start of the list */
    private final int offset;

    /** {@code non-null;} attribute factory to use */
    private final AttributeFactory attributeFactory;

    /** {@code >= -1;} the end offset of this list in the byte array of the
     * classfile, or {@code -1} if not yet parsed */
    private int endOffset;

    /**
     * Constructs an instance.
     *
     * @param cf {@code non-null;} the class file to parse from
     * @param definer {@code non-null;} class being defined
     * @param offset offset in {@code bytes} to the start of the list
     * @param attributeFactory {@code non-null;} attribute factory to use
     */
    MemberListParser(DirectClassFile cf, CstType definer,
                     int offset, AttributeFactory attributeFactory) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        }

        if (attributeFactory == null) {
            throw new NullPointerException("attributeFactory == null");
        }

        this.cf = cf;
        this.definer = definer;
        this.offset = offset;
        this.attributeFactory = attributeFactory;
        this.endOffset = -1;
    }

    /**
     * Gets the end offset of this constant pool in the {@code byte[]}
     * which it came from.
     *
     * @return {@code >= 0;} the end offset
     */
    public int getEndOffset() {
        parseIfNecessary();
        return endOffset;
    }

    /**
     * Runs {@link #parse} if it has not yet been run successfully.
     */
    final void parseIfNecessary() {
        if (endOffset < 0) {
            parse();
        }
    }

    /**
     * Gets the count of elements in the list.
     *
     * @return the count
     */
    final int getCount() {
        ByteArray bytes = cf.getBytes();
        return bytes.getUnsignedShort(offset);
    }

    /**
     * Gets the class file being defined.
     *
     * @return {@code non-null;} the class
     */
    final CstType getDefiner() {
        return definer;
    }

    /**
     * Gets the human-oriented name for what this instance is parsing.
     * Subclasses must override this method.
     *
     * @return {@code non-null;} the human oriented name
     */
    protected abstract String humanName();

    /**
     * Gets the {@code CTX_*} constant to use when parsing attributes.
     * Subclasses must override this method.
     *
     * @return {@code non-null;} the human oriented name
     */
    protected abstract int getAttributeContext();

    /**
     * Sets an element in the list. Subclasses must override this method.
     *
     * @param n which element
     * @param accessFlags the {@code access_flags}
     * @param nat the interpreted name and type (based on the two
     * {@code *_index} fields)
     * @param attributes list of parsed attributes
     * @return {@code non-null;} the constructed member
     */
    protected abstract Member set(int n, int accessFlags, CstNat nat,
            AttributeList attributes);

    /**
     * Does the actual parsing.
     */
    private void parse() {
        int attributeContext = getAttributeContext();
        int count = getCount();
        int at = offset + 2; // Skip the count.

        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();

        for (int i = 0; i < count; i++) {
            try {
                int accessFlags = bytes.getUnsignedShort(at);
                int nameIdx = bytes.getUnsignedShort(at + 2);
                int descIdx = bytes.getUnsignedShort(at + 4);
                CstString name = (CstString) pool.get(nameIdx);
                CstString desc = (CstString) pool.get(descIdx);

                at += 6;
                AttributeListParser parser =
                    new AttributeListParser(cf, attributeContext, at,
                                            attributeFactory);
                at = parser.getEndOffset();
                StdAttributeList attributes = parser.getList();
                attributes.setImmutable();
                CstNat nat = new CstNat(name, desc);
                Member member = set(i, accessFlags, nat, attributes);
            } catch (ParseException ex) {
                ex.addContext("...while parsing " + humanName() + "s[" + i +
                              "]");
                throw ex;
            } catch (RuntimeException ex) {
                ParseException pe = new ParseException(ex);
                pe.addContext("...while parsing " + humanName() + "s[" + i +
                              "]");
                throw pe;
            }
        }

        endOffset = at;
    }
}
