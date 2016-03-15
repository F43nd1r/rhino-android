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

/**
 * Interface for lists of methods.
 */
public interface MethodList {

    /**
     * Get the number of methods in the list.
     *
     * @return the size
     */
    int size();

    /**
     * Get the {@code n}th method.
     *
     * @param n {@code n >= 0, n < size();} which method
     * @return {@code non-null;} the method in question
     */
    Method get(int n);
}
