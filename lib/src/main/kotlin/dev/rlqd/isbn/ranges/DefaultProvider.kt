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

import kotlinx.serialization.json.Json
import org.jetbrains.annotations.ApiStatus

/**
 * Provider that loads ranges from the library jar.
 *
 * ISBN Ranges are downloaded, converted and bundled with the library jar during build.
 * This way the library can be used without worrying about getting ranges information
 * at an expense of using potentially outdated ranges.
 *
 * If you rely on recent ranges information, consider using OnlineProvider or implementing your own.
 */
@ApiStatus.AvailableSince("1.0.0")
class DefaultProvider(eagerLoad: Boolean = true): Provider {

    private val eagerMap: Map<String,RangeGroup>?
        = if (eagerLoad) loadMap() else null

    private val map: Map<String,RangeGroup> by lazy {
        eagerMap ?: loadMap()
    }

    private fun loadMap(): Map<String,RangeGroup> {
        val resource = this::class.java.getResourceAsStream("isbn-ranges.json")
        if (resource === null) {
            throw RuntimeException("Failed to load ISBN ranges from classpath (resource not found)")
        }
        return resource.reader().use {
            Json.decodeFromString<Map<String,RangeGroup>>(it.readText())
        }
    }

    override fun getRanges(prefix: String): RangeGroup? {
        return map[prefix]
    }
}