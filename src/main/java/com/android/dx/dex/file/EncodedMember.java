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

import com.android.dx.util.AnnotatedOutput;
import com.android.dx.util.ToHuman;

/**
 * Representation of a member (field or method) of a class, for the
 * purposes of encoding it inside a {@link ClassDataItem}.
 */
abstract class EncodedMember implements ToHuman {
    /** access flags */
    private final int accessFlags;

    /**
     * Constructs an instance.
     *
     * @param accessFlags access flags for the member
     */
    EncodedMember(int accessFlags) {
        this.accessFlags = accessFlags;
    }

    /**
     * Gets the access flags.
     *
     * @return the access flags
     */
    final int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Encodes this instance to the given output.
     *
     * @param file {@code non-null;} file this instance is part of
     * @param out {@code non-null;} where to write to
     * @param lastIndex {@code >= 0;} the previous member index value encoded, or
     * {@code 0} if this is the first element to encode
     * @param dumpSeq {@code >= 0;} sequence number of this instance for
     * annotation purposes
     * @return {@code >= 0;} the member index value that was encoded
     */
    public abstract int encode(DexFile file, AnnotatedOutput out,
            int lastIndex, int dumpSeq);
}
