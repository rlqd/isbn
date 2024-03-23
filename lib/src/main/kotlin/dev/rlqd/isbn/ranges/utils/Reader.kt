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

import dev.rlqd.isbn.ranges.IllegalRangeException
import dev.rlqd.isbn.ranges.Range
import dev.rlqd.isbn.ranges.RangeGroup
import org.jetbrains.annotations.ApiStatus
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

/**
 * This object transforms input stream (containing ISBN RangeMessage in XML format from isbn-international.org)
 * into a serializable structure usable by library.
 *
 * It uses Java StAX API (XMLStreamReader).
 */
@ApiStatus.Experimental
object Reader {
    class ReadException(msg: String, cause: Throwable? = null): Exception(msg, cause)

    fun read(stream: InputStream): Map<String, RangeGroup> {
        val map = mutableMapOf<String, RangeGroup>()
        val xmlFactory = XMLInputFactory.newFactory()
        val reader = xmlFactory.createXMLStreamReader(stream)
        while (reader.hasNext()) {
            if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                if (reader.localName == "EAN.UCC" || reader.localName == "Group") {
                    val entry = readGroup(reader)
                    map[entry.first] = entry.second
                }
            }
        }
        return map
    }

    private fun readGroup(reader: XMLStreamReader): Pair<String, RangeGroup>
    {
        val groupNodeName = reader.localName
        var prefix: String? = null
        var name: String? = null
        val ranges = mutableListOf<Range>()
        while (reader.hasNext()) {
            when (reader.next()) {
                XMLStreamConstants.END_ELEMENT -> {
                    if (reader.localName == groupNodeName) {
                        break
                    }
                }
                XMLStreamConstants.START_ELEMENT -> {
                    when (reader.localName) {
                        "Prefix" -> prefix = readText(reader)
                        "Agency" -> name = readText(reader)
                        "Rule" -> ranges.add(readRange(reader, prefix))
                    }
                }
            }
        }
        if (prefix.isNullOrEmpty() || name.isNullOrEmpty() || ranges.isEmpty()) {
            throw ReadException("Can't parse incomplete group at prefix '$prefix'")
        }
        return Pair(prefix, RangeGroup(name, ranges))
    }

    private fun readRange(reader: XMLStreamReader, prefix: String?): Range
    {
        var rangeStr: String? = null
        var length: String? = null
        while(reader.hasNext()) {
            when(reader.next()) {
                XMLStreamConstants.END_ELEMENT -> {
                    if (reader.localName == "Rule") {
                        break
                    }
                }
                XMLStreamConstants.START_ELEMENT -> {
                    when(reader.localName) {
                        "Range" -> rangeStr = readText(reader)
                        "Length" -> length = readText(reader)
                    }
                }
            }
        }
        if (rangeStr.isNullOrEmpty() || length.isNullOrEmpty()) {
            throw ReadException("Can't parse incomplete rule at prefix '$prefix'")
        }
        val rangeParts = rangeStr.split('-')
        if (rangeParts.size != 2) {
            throw ReadException("Malformed range '$rangeStr' at prefix '$prefix'")
        }
        try {
            with(rangeParts.map { it.toInt() }) {
                return Range(get(0), get(1), length.toInt())
            }
        } catch (e: Throwable) {
            throw when (e) {
                is NumberFormatException, is IllegalRangeException -> ReadException("Malformed range '$rangeStr' at prefix '$prefix'", e)
                else -> e
            }
        }
    }

    private fun readText(reader: XMLStreamReader): String?
    {
        if (reader.hasNext() && reader.next() == XMLStreamConstants.CHARACTERS) {
            return reader.text
        }
        return null
    }
}