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

import com.android.dx.cf.attrib.RawAttribute;
import com.android.dx.cf.iface.Attribute;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.rop.cst.ConstantPool;
import com.android.dx.rop.cst.CstString;
import com.android.dx.util.ByteArray;
import com.android.dx.util.Hex;

/**
 * Factory capable of instantiating various {@link Attribute} subclasses
 * depending on the context and name.
 */
class AttributeFactory {
    /** context for attributes on class files */
    public static final int CTX_CLASS = 0;

    /** context for attributes on fields */
    public static final int CTX_FIELD = 1;

    /** context for attributes on methods */
    public static final int CTX_METHOD = 2;

    /** context for attributes on code attributes */
    static final int CTX_CODE = 3;

    /** number of contexts */
    private static final int CTX_COUNT = 4;

    /**
     * Constructs an instance.
     */
    AttributeFactory() {
        // This space intentionally left blank.
    }

    /**
     * Parses and makes an attribute based on the bytes at the
     * indicated position in the given array. This method figures out
     * the name, and then does all the setup to call on to {@link #parse0},
     * which does the actual construction.
     *
     * @param cf {@code non-null;} class file to parse from
     * @param context context to parse in; one of the {@code CTX_*}
     * constants
     * @param offset offset into {@code dcf}'s {@code bytes}
     * to start parsing at
     * @return {@code non-null;} an appropriately-constructed {@link Attribute}
     */
    public final Attribute parse(DirectClassFile cf, int context, int offset) {
        if (cf == null) {
            throw new NullPointerException("cf == null");
        }

        if ((context < 0) || (context >= CTX_COUNT)) {
            throw new IllegalArgumentException("bad context");
        }

        CstString name = null;

        try {
            ByteArray bytes = cf.getBytes();
            ConstantPool pool = cf.getConstantPool();
            int nameIdx = bytes.getUnsignedShort(offset);
            int length = bytes.getInt(offset + 2);

            name = (CstString) pool.get(nameIdx);

            return parse0(cf, context, name.getString(), offset + 6, length);
        } catch (ParseException ex) {
            ex.addContext("...while parsing " +
                    ((name != null) ? (name.toHuman() + " ") : "") +
                    "attribute at offset " + Hex.u4(offset));
            throw ex;
        }
    }

    /**
     * Parses attribute content. The base class implements this by constructing
     * an instance of {@link RawAttribute}. Subclasses are expected to
     * override this to do something better in most cases.
     *
     * @param cf {@code non-null;} class file to parse from
     * @param context context to parse in; one of the {@code CTX_*}
     * constants
     * @param name {@code non-null;} the attribute name
     * @param offset offset into {@code bytes} to start parsing at; this
     * is the offset to the start of attribute data, not to the header
     * @param length the length of the attribute data
     * @return {@code non-null;} an appropriately-constructed {@link Attribute}
     */
    Attribute parse0(DirectClassFile cf, int context, String name,
                     int offset, int length) {
        ByteArray bytes = cf.getBytes();
        ConstantPool pool = cf.getConstantPool();

        return new RawAttribute(name, bytes, offset, length);
    }
}
