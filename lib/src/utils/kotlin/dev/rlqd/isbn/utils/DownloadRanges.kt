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

package dev.rlqd.isbn.utils

import dev.rlqd.isbn.ranges.utils.DownloadClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * This file is used as a part of build process and excluded from the library distribution.
 * It downloads and serializes ISBN ranges at the time of build to use as a default/backup option.
 */

fun main() {
    val outFile = File("build/external/isbn-ranges.json")
    println("Output file: ${outFile.absolutePath}")
    if (outFile.exists()) {
        println("File already exists, exiting.")
        return
    }

    println("Starting ranges download")
    val client = DownloadClient()
    val ranges = client.download()
    val rangesStr = Json.encodeToString(ranges)

    println("Saving to output file")
    outFile.parentFile.mkdir()
    outFile.writeText(rangesStr)
    println("Done!")
}