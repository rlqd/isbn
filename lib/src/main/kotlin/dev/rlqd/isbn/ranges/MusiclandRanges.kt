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

@ApiStatus.Internal
internal object MusiclandRanges {
    const val PREFIX = "979-0"
    const val EAN_PREFIX = "9790"

    val group = RangeGroup(
        name = "Musicland",
        list = listOf(
            Range(0, 999999, 3),        /* 000 - 099 */
            Range(1000000, 3999999, 4), /* 1000 - 3999 */
            Range(4000000, 6999999, 5), /* 40000 - 69999 */
            Range(7000000, 8999999, 6), /* 700000 - 899999 */
            Range(9000000, 9999999, 7), /* 9000000 - 9999999 */
        ),
    )
}