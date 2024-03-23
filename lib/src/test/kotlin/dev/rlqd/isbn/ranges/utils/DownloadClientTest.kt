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

package dev.rlqd.isbn.ranges.utils

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadClientTest {
    @Test
    fun clientSuccess() {
        val http = MockHttpClient(
            { FileInputStream("src/test/resources/ranges-information.json") },
            { FileInputStream("src/test/resources/isbn-ranges-ok.xml") },
        )

        assertDoesNotThrow {
            DownloadClient(http).download()
        }

        assertEquals(1, http.postCalls.count(), "A single POST request is expected")
        assertEquals(
            Pair(
                URI("https://www.isbn-international.org/bl_proxy/GetRangeInformations"),
                "format=1&language=en&translatedTexts=Printed%3BLast%20Change",
            ),
            http.postCalls.first(),
            "Wrong POST request URI or data",
        )

        assertEquals(1, http.getCalls.count(), "A single GET request is expected")
        assertEquals(
            URI("https://www.isbn-international.org/download_range/15821/RangeMessage.xml"),
            http.getCalls.first(),
            "Wrong GET URI",
        )
    }

    @Test
    fun clientError() {
        val http = MockHttpClient(
            { FileInputStream("src/test/resources/ranges-error-response.json") },
            { FileInputStream("src/test/resources/isbn-ranges-ok.xml") },
        )

        val ex = assertThrows<DownloadClient.DownloadException> {
            DownloadClient(http).download()
        }
        assertEquals("GetRangesInformation was unsuccessful (status=error; message=some error)", ex.message)

        assertEquals(1, http.postCalls.count(), "A single POST request is expected")
        assertEquals(0, http.getCalls.count(), "No GET requests are expected")
    }
}

private class MockHttpClient(
    private val handlePost: () -> InputStream,
    private val handleGet: () -> InputStream,
): DownloadClient.HttpClient
{
    val postCalls = mutableListOf<Pair<URI,String>>()
    val getCalls = mutableListOf<URI>()

    override fun <T> runPost(uri: URI, formData: String, streamHandler: (InputStream) -> T): T {
        postCalls.add(Pair(uri, formData))
        return handlePost().use(streamHandler)
    }

    override fun <T> runGet(uri: URI, streamHandler: (InputStream) -> T): T {
        getCalls.add(uri)
        return handleGet().use(streamHandler)
    }
}
