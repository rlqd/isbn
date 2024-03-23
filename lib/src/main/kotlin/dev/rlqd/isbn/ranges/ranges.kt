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

import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus

@Serializable
@ApiStatus.AvailableSince("1.0.0")
data class RangeGroup(
    val name: String,
    val list: List<Range>,
) {
    fun findRange(index: Int): Range?
        = list.firstOrNull { it.start <= index && index <= it.end }
}

@Serializable
data class Range(
    val start: Int,
    val end: Int,
    val length: Int,
) {
    init {
        if (start < 0 || end < 0 || start > end) {
            throw IllegalRangeException(start, end)
        }
    }
}

class IllegalRangeException(start: Int, end: Int): RuntimeException("Invalid range start:$start, end:$end")
