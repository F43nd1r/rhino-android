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

package com.android.dx.cf.attrib;

import com.android.dx.util.ByteArray;

/**
 * Raw attribute, for holding onto attributes that are unrecognized.
 */
public final class RawAttribute extends BaseAttribute {
    /** {@code non-null;} attribute data */
    private final ByteArray data;

    /**
     * Constructs an instance.
     *  @param name {@code non-null;} attribute name
     * @param data {@code non-null;} attribute data
     */
    private RawAttribute(String name, ByteArray data) {
        super(name);

        if (data == null) {
            throw new NullPointerException("data == null");
        }

        this.data = data;
    }

    /**
     * Constructs an instance from a sub-array of a {@link ByteArray}.
     *  @param name {@code non-null;} attribute name
     * @param data {@code non-null;} array containing the attribute data
     * @param offset offset in {@code data} to the attribute data
     * @param length length of the attribute data, in bytes
     */
    public RawAttribute(String name, ByteArray data, int offset,
                        int length) {
        this(name, data.slice(offset, offset + length));
    }

    /** {@inheritDoc} */
    public int byteLength() {
        return data.size() + 6;
    }

}
