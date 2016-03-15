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

package com.android.dx.dex.file;

import com.android.dex.Leb128;
import com.android.dx.rop.code.AccessFlags;
import com.android.dx.rop.cst.CstFieldRef;
import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.Hex;

/**
 * Representation of a field of a class, of any sort.
 */
public final class EncodedField extends EncodedMember
        implements Comparable<EncodedField> {
    /** {@code non-null;} constant for the field */
    private final CstFieldRef field;

    /**
     * Constructs an instance.
     *
     * @param field {@code non-null;} constant for the field
     * @param accessFlags access flags
     */
    public EncodedField(CstFieldRef field, int accessFlags) {
        super(accessFlags);

        if (field == null) {
            throw new NullPointerException("field == null");
        }

        /*
         * TODO: Maybe check accessFlags, at least for
         * easily-checked stuff?
         */

        this.field = field;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return field.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object other) {
        return other instanceof EncodedField && compareTo((EncodedField) other) == 0;

    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Note:</b> This compares the method constants only,
     * ignoring any associated code, because it should never be the
     * case that two different items with the same method constant
     * ever appear in the same list (or same file, even).</p>
     */
    public int compareTo(EncodedField other) {
        return field.compareTo(other.field);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String sb = getClass().getName() +
                '{' +
                Hex.u2(getAccessFlags()) +
                ' ' +
                field +
                '}';

        return sb;
    }

    /** {@inheritDoc} */
    public void addContents(DexFile file) {
        FieldIdsSection fieldIds = file.getFieldIds();
        fieldIds.intern(field);
    }

    /** {@inheritDoc} */
    public String toHuman() {
        return field.toHuman();
    }

    /**
     * Gets the constant for the field.
     *
     * @return {@code non-null;} the constant
     */
    public CstFieldRef getRef() {
        return field;
    }

    /** {@inheritDoc} */
    @Override
    public int encode(DexFile file, AnnotatedOutput out,
            int lastIndex, int dumpSeq) {
        int fieldIdx = file.getFieldIds().indexOf(field);
        int diff = fieldIdx - lastIndex;
        int accessFlags = getAccessFlags();

        if (out.annotates()) {
            out.annotate(0, String.format("  [%x] %s", dumpSeq,
                            field.toHuman()));
            out.annotate(Leb128.unsignedLeb128Size(diff),
                    "    field_idx:    " + Hex.u4(fieldIdx));
            out.annotate(Leb128.unsignedLeb128Size(accessFlags),
                    "    access_flags: " +
                    AccessFlags.fieldString(accessFlags));
        }

        out.writeUleb128(diff);
        out.writeUleb128(accessFlags);

        return fieldIdx;
    }
}
