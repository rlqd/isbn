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

import dev.rlqd.isbn.ranges.RangeGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.ApiStatus
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI

/**
 * !NB! This method relies on an unofficial api for obtaining the ranges (emulates html form on isbn-international.org).
 * There is no clear understanding of request limits and no guarantee that the endpoint will remain unchanged.
 * Use with care and ensure backup options available in critical applications.
 *
 * By default, the client is created with quite relaxed timeouts (10 sec for connect, 60 sec for read timeout).
 * You may use a secondary constructor to specify custom timeouts.
 */
@ApiStatus.Experimental
class DownloadClient(
    private val httpClient: HttpClient = DefaultHttpClient(10000, 60000),
) {
    constructor(connectTimeout: Int, readTimeout: Int): this(DefaultHttpClient(connectTimeout, readTimeout))

    class DownloadException(msg: String, cause: Throwable? = null): Exception(msg, cause)

    /**
     * HttpClient is used to locate and download latest ranges information (making several GET/POST requests).
     * When implementing custom HttpClient, you should handle http error codes.
     *
     * When subclasses of IOException are thrown, they are handled by DownloadClient and re-thrown as DownloadException.
     * Any other exception types will be left uncatched.
     */
    interface HttpClient {
        fun <T> runPost(uri: URI, formData: String, streamHandler: (InputStream) -> T): T
        fun <T> runGet(uri: URI, streamHandler: (InputStream) -> T): T
    }

    private class DefaultHttpClient(val userConnectTimeout: Int, val userReadTimeout: Int): HttpClient
    {
        class HttpException(msg: String): IOException(msg)

        override fun <T> runPost(uri: URI, formData: String, streamHandler: (InputStream) -> T): T {
            with(uri.toURL().openConnection() as HttpURLConnection) {
                try {
                    connectTimeout = userConnectTimeout
                    readTimeout = userReadTimeout
                    requestMethod = "POST"
                    doOutput = true

                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    setRequestProperty("Content-Length", formData.length.toString())

                    // Send the POST data
                    val writer = OutputStreamWriter(outputStream)
                    writer.write(formData)
                    writer.flush()

                    val code = responseCode
                    if (code != 200) {
                        throw HttpException("Http response code $code")
                    }
                    return streamHandler(inputStream)
                } finally {
                    inputStream.close()
                    disconnect()
                }
            }
        }

        override fun <T> runGet(uri: URI, streamHandler: (InputStream) -> T): T {
            with(uri.toURL().openConnection() as HttpURLConnection) {
                try {
                    connectTimeout = userConnectTimeout
                    readTimeout = userReadTimeout
                    requestMethod = "GET"
                    doOutput = true

                    val code = responseCode
                    if (code != 200) {
                        throw HttpException("Http response code $code")
                    }
                    return streamHandler(inputStream)
                } finally {
                    inputStream.close()
                    disconnect()
                }
            }
        }
    }

    @Serializable
    private data class RangeInformationResponse(
        val result: RangeInformation?,
        val status: String,
        val messages: List<String>,
    ) {
        @Serializable
        data class RangeInformation(
            val value: String,
            val filename: String,
        )
    }

    @Throws(DownloadException::class, Reader.ReadException::class)
    fun download(): Map<String, RangeGroup> {
        val json = Json {
            ignoreUnknownKeys = true
        }
        val info = try {
            httpClient.runPost(
                URI("https://www.isbn-international.org/bl_proxy/GetRangeInformations"),
                "format=1&language=en&translatedTexts=Printed%3BLast%20Change",
            ) {
                json.decodeFromString(RangeInformationResponse.serializer(), it.bufferedReader().readText())
            }
        } catch (e: Throwable) {
            throw DownloadException("Error occurred while trying to do HTTP request GetRangesInformation", e)
        }
        if (info.status != "success" || info.result === null) {
            throw DownloadException("GetRangesInformation was unsuccessful (status=${info.status}; message=${info.messages.joinToString()})")
        }
        val rangeUri = URI("https://www.isbn-international.org/download_range/${info.result.value}/${info.result.filename}")
        try {
            return httpClient.runGet(rangeUri) {
                Reader.read(it)
            }
        } catch (e: IOException) {
            throw DownloadException("Error occurred while trying to do HTTP xml download request", e)
        }
    }
}