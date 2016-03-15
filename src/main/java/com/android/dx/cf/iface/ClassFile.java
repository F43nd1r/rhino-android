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

package com.android.dx.cf.iface;

import com.android.dx.rop.cst.CstString;

/**
 * Interface for things which purport to be class files or reasonable
 * facsimiles thereof.
 *
 * <p><b>Note:</b> The fields referred to in this documentation are of the
 * {@code ClassFile} structure defined in vmspec-2 sec4.1.
 */
public interface ClassFile extends HasAttribute {

    /**
     * Gets the field {@code access_flags}.
     *
     * @return the value in question
     */
    int getAccessFlags();

    /**
     * Gets the name out of the {@code SourceFile} attribute of this
     * file, if any. This is a convenient shorthand for scrounging around
     * the class's attributes.
     *
     * @return {@code non-null;} the constant pool
     */
    CstString getSourceFile();
}
