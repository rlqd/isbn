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

import dev.rlqd.isbn.ranges.cache.InMemoryCache
import dev.rlqd.isbn.ranges.utils.DownloadClient
import kotlinx.coroutines.*
import org.junit.jupiter.api.assertThrows
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

class OnlineProviderTest {
    @Test
    fun failed() {
        val http = MockHttpClient(false)
        http.resume()
        val provider = OnlineProvider(InMemoryCache(), DownloadClient(http))
        assertThrows<RuntimeException> {
            provider.getGs1Ranges(978u)
        }.let {
            assertEquals("OnlineProvider is unusable - cache is empty and download has failed", it.message)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun inProgress() {
        val http = MockHttpClient()
        val provider = OnlineProvider(InMemoryCache(), DownloadClient(http))
        val job = GlobalScope.launch {
            try {
                if (provider.getGs1Ranges(978u) === null) {
                    cancel("No ranges provided")
                }
            } catch (e: Throwable) {
                cancel("Failed with exception", e)
            }
        }
        http.resume()
        runBlocking {
            withTimeout(1000) {
                job.join()
            }
        }
        assertTrue(job.isCompleted)
        assertFalse(job.isCancelled, "Job must complete successfully, but it was cancelled")
    }

    @Test
    fun ready() {
        val http = MockHttpClient()
        http.resume()
        val cache = InMemoryCache()

        val provider = OnlineProvider(cache, DownloadClient(http))
        provider.waitBlocking()

        assertTrue(cache.isExists, "Cache must exist at this point")
        assertNotNull(provider.getGs1Ranges(978u), "Provider doesn't provide")
    }
}

private class MockHttpClient(private val success: Boolean = true): DownloadClient.HttpClient {
    private val latch = CountDownLatch(1)

    fun resume() {
        latch.countDown()
    }

    override fun <T> runPost(uri: URI, formData: String, streamHandler: (InputStream) -> T): T {
        latch.await(5, TimeUnit.SECONDS)
        if (!success) {
            throw IOException("Something went wrong")
        }
        return FileInputStream("src/test/resources/ranges-information.json").use(streamHandler)
    }

    override fun <T> runGet(uri: URI, streamHandler: (InputStream) -> T): T
        = FileInputStream("src/test/resources/isbn-ranges-ok.xml").use(streamHandler)
}
