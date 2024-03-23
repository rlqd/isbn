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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.nio.file.Files
import kotlin.math.max

/**
 * By default, saves to rlqd-isbn-ranges-cache.json file in system tmp directory.
 */
@ApiStatus.Experimental
class FileCache(
    private val ttl: Long = 86400000L * 30,
    filePath: String? = null,
): Cache {
    companion object Singleton {
        val default: FileCache by lazy { FileCache() }
    }

    private val file = File(filePath ?: "${System.getProperty("java.io.tmpdir")}/rlqd-isbn-ranges-cache.json")
    private var lastModified: Long = 0
    private var internalCache: Map<String, RangeGroup>? = null

    override val isExists
        get() = internalCache != null || file.exists()
    override val validFor
        get() = max(0, lastModified + ttl - System.currentTimeMillis())

    override fun load(): Map<String, RangeGroup> {
        if (internalCache === null) {
            if (!isExists) {
                throw RuntimeException("Trying to load non existing cache")
            }
            val cacheStr = String(Files.readAllBytes(file.toPath()))
            internalCache = Json.decodeFromString(cacheStr)
            lastModified = file.lastModified()
        }
        return internalCache!!
    }

    override fun save(map: Map<String, RangeGroup>) {
        internalCache = map
        Files.write(file.toPath(), Json.encodeToString(map).toByteArray())
        lastModified = file.lastModified()
    }
}