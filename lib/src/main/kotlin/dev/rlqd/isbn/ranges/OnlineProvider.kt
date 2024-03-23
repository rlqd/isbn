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

import dev.rlqd.isbn.ranges.cache.Cache
import dev.rlqd.isbn.ranges.cache.FileCache
import dev.rlqd.isbn.ranges.utils.DownloadClient
import kotlinx.coroutines.*
import org.jetbrains.annotations.ApiStatus
import java.util.*
import kotlin.concurrent.schedule
import kotlin.coroutines.cancellation.CancellationException

/**
 * !NB! This provider uses unofficial api for obtaining the ranges (emulates html form).
 * Never rely on it alone in critical applications. Read more in DownloadClient.
 *
 * Provider that downloads ranges from isbn-international.org.
 * Backed by file cache by default with TTL of 30 days.
 * It eagerly starts download in background when constructed if cache doesn't exist.
 * By default, timeouts are quite relaxed (10s connect, 60s read; see DownloadClient).
 *
 * Ranges are re-downloaded by daemon thread automatically when cache expires.
 * Outdated cache may be used until new ranges are available, ensuring no disruption to service operation.
 *
 * It may be a good choice for small one-instance services, balancing between maintenance cost and relevance of ranges.
 * For distributed systems consider implementing a custom provider with distributed cache and dedicated update service.
 */
@ApiStatus.Experimental
class OnlineProvider(
    private val cache: Cache = FileCache.default,
    private val client: DownloadClient = DownloadClient(),
): Provider {

    private var preloadJob: Job? = null
    private val refreshTimer = Timer("ISBN cache refresh", true)

    init {
        if (cache.isExists) {
            cache.load()
            scheduleRefresh()
        } else {
            preloadJob = refreshCache()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun refreshCache() = GlobalScope.launch(Dispatchers.IO) {
        try {
            cache.save(client.download())
            scheduleRefresh()
        } catch (e: Throwable) {
            cancel("Download has failed with exception", e)
        }
    }

    private fun scheduleRefresh() {
        cache.validFor.let {
            if (it > 0 && it != Long.MAX_VALUE) {
                refreshTimer.schedule(it) {
                    refreshCache()
                }
            }
        }
    }

    /**
     * When cache exists:
     * Run cache.load() for the first time to optimise subsequent calls
     *
     * When cache doesn't exist:
     * Start preload if it's not started yet and wait for it to finish
     */
    suspend fun wait() {
        if (preloadJob === null) {
            if (!cache.isExists) {
                throw RuntimeException("OnlineProvider wrong state - cache is empty and download not started")
            }
            return
        }
        if (preloadJob?.isActive == true) {
            preloadJob?.join()
        }
        if (preloadJob?.isCancelled == true) {
            throw RuntimeException("OnlineProvider is unusable - cache is empty and download has failed")
        }
    }

    /**
     * Similar to wait(), but blocking.
     * Blocks current thread until preload is finished.
     */
    fun waitBlocking() = runBlocking { wait() }

    override fun getRanges(prefix: String): RangeGroup? {
        if (!cache.isExists) {
            waitBlocking()
        }
        return cache.load()[prefix]
    }
}