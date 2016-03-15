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

package com.android.dx.rop.code;

import com.android.dx.util.FixedSizeList;

/**
 * List of {@link Insn} instances.
 */
public final class InsnList
        extends FixedSizeList {
    /**
     * Constructs an instance. All indices initially contain {@code null}.
     *
     * @param size the size of the list
     */
    public InsnList(int size) {
        super(size);
    }

    /**
     * Gets the element at the given index. It is an error to call
     * this with the index for an element which was never set; if you
     * do that, this will throw {@code NullPointerException}.
     *
     * @param n {@code >= 0, < size();} which index
     * @return {@code non-null;} element at that index
     */
    public Insn get(int n) {
        return (Insn) get0(n);
    }

    /**
     * Sets the instruction at the given index.
     *
     * @param n {@code >= 0, < size();} which index
     * @param insn {@code non-null;} the instruction to set at {@code n}
     */
    public void set(int n, Insn insn) {
        set0(n, insn);
    }

    /**
     * Gets the last instruction. This is just a convenient shorthand for
     * {@code get(size() - 1)}.
     *
     * @return {@code non-null;} the last instruction
     */
    public Insn getLast() {
        return get(size() - 1);
    }

    /**
     * Visits each instruction in the list, in order.
     *
     * @param visitor {@code non-null;} visitor to use
     */
    public void forEach(Insn.Visitor visitor) {
        int sz = size();

        for (int i = 0; i < sz; i++) {
            get(i).accept(visitor);
        }
    }

    /**
     * Compares the contents of this {@code InsnList} with another.
     * The blocks must have the same number of insns, and each Insn must
     * also return true to {@code Insn.contentEquals()}.
     *
     * @param b to compare
     * @return true in the case described above.
     */
    public boolean contentEquals(InsnList b) {
        if (b == null) return false;

        int sz = size();

        if (sz != b.size()) return false;

        for (int i = 0; i < sz; i++) {
            if (!get(i).contentEquals(b.get(i))) {
                return false;
            }
        }

        return true;
    }

}
