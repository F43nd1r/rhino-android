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

package com.android.dx.rop.type;

/**
 * List of {@link Type} instances (or of things that contain types).
 */
public interface TypeList {
    /**
     * Returns whether this instance is mutable. Note that the
     * {@code TypeList} interface itself doesn't provide any
     * means of mutation, but that doesn't mean that there isn't an
     * extra-interface way of mutating an instance.
     *
     * @return {@code true} if this instance is mutable or
     * {@code false} if it is immutable
     */
    boolean isMutable();

    /**
     * Gets the size of this list.
     *
     * @return {@code >= 0;} the size
     */
    int size();

    /**
     * Gets the indicated element. It is an error to call this with the
     * index for an element which was never set; if you do that, this
     * will throw {@code NullPointerException}.
     *
     * @param n {@code >= 0, < size();} which element
     * @return {@code non-null;} the indicated element
     */
    Type getType(int n);

    /**
     * Returns a new instance which is identical to this one, except that
     * the given item is appended to the end and it is guaranteed to be
     * immutable.
     *
     * @param type {@code non-null;} item to append
     * @return {@code non-null;} an appropriately-constructed instance
     */
    TypeList withAddedType(Type type);
}
