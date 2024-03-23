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

package dev.rlqd.isbn.ranges.cache

import dev.rlqd.isbn.ranges.RangeGroup
import org.jetbrains.annotations.ApiStatus
import kotlin.math.max

/**
 * Holds value in the process memory. Does not expire by default.
 * It will re-download ranges on each process restart.
 *
 * Note: generally not recommended to use, unless you have a very specific case.
 * e.g. you launch a script once a month to process new books, and you need the most recent ranges information.
 */
@ApiStatus.Experimental
class InMemoryCache(
    private val ttl: Long = Long.MAX_VALUE,
): Cache {
    private var internalCache: Map<String, RangeGroup>? = null
    private var lastModified: Long = 0

    override val isExists: Boolean
        get() = internalCache != null

    override val validFor
        get() = if (ttl == Long.MAX_VALUE) Long.MAX_VALUE
            else max(0, lastModified + ttl - System.currentTimeMillis())

    override fun load(): Map<String, RangeGroup> {
        if (!isExists) {
            throw RuntimeException("Trying to load non existing cache")
        }
        return internalCache!!
    }

    override fun save(map: Map<String, RangeGroup>) {
        internalCache = map
        lastModified = System.currentTimeMillis()
    }
}