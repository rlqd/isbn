/*
 * Copyright 2024 https://rlqd.dev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.rlqd.isbn.ranges

import org.jetbrains.annotations.ApiStatus

/**
 * Provides the ranges rules to use for ISBN validation.
 */
@ApiStatus.AvailableSince("1.0.0")
interface Provider {
    /**
     * This method returns ranges for registration groups.
     * Prefix for registration group = gs1 + "-" + regGroup (example: "978-1")
     */
    fun getGroupRanges(gs1: UShort, regGroup: UInt): RangeGroup?
        = getRanges("$gs1-$regGroup")

    /**
     * This method returns ranges for gs1 (EAN.UCC).
     * Prefix for gs1 ranges is a string representation of a gs1 number (example: "978")
     */
    fun getGs1Ranges(gs1: UShort)
        = getRanges(gs1.toString())

    /**
     * Returns ranges for either gs1 or registration groups depending on the prefix.
     * See other methods for more details.
     */
    fun getRanges(prefix: String): RangeGroup?
}