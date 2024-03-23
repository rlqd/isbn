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

/**
 * Cache for the ranges OnlineProvider.
 */
@ApiStatus.Experimental
interface Cache {
    /**
     * Does the cache exists (e.g. file exists).
     * If this method returns false, provider must populate the cache before calling load().
     */
    val isExists: Boolean

    /**
     * How long the cache will be valid for (milliseconds).
     * May return 0 until load() or save() was called for the first time.
     *
     * NB when implementing new cache:
     * Never remove invalid cache to avoid blocking the thread that will use the library.
     * Provider must handle this instead and "override" the invalid cache in background.
     */
    val validFor: Long

    /**
     * Get the stored value.
     * Cache holds the value in memory, subsequent calls are faster.
     * Throws exception if called on non-existing cache, see getIsExists().
     */
    @Throws(RuntimeException::class)
    fun load(): Map<String, RangeGroup>

    /**
     * Store the new value.
     */
    fun save(map: Map<String, RangeGroup>)
}