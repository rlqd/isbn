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

import dev.rlqd.isbn.ranges.Range
import dev.rlqd.isbn.ranges.RangeGroup
import org.junit.jupiter.api.assertThrows
import java.io.FileInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderTest {
    @Test
    fun readsCorrectRanges() {
        FileInputStream("src/test/resources/isbn-ranges-ok.xml").use {
            val result = Reader.read(it)
            assertEquals(
                mapOf(
                    "978" to RangeGroup(
                        "International ISBN Agency",
                        listOf(
                            Range(0,5999999,1),
                            Range(6000000, 6499999,3),
                        ),
                    ),
                    "979" to RangeGroup(
                        "International ISBN Agency",
                        listOf(
                            Range(0,999999,0),
                            Range(1000000, 1599999,2),
                        ),
                    ),
                    "978-0" to RangeGroup(
                        "English language",
                        listOf(
                            Range(0,1999999,2),
                            Range(2000000, 2279999,3),
                        ),
                    ),
                    "978-1" to RangeGroup(
                        "English language 2",
                        listOf(
                            Range(0,99999,3),
                            Range(9989900, 9999999,7),
                        ),
                    ),
                ),
                result,
            )
        }
    }

    @Test
    fun handlesWrongXml() {
        mapOf(
            "src/test/resources/isbn-ranges-malformed-range.xml" to "Malformed range '6000000-6499999-xyz' at prefix '978'",
            "src/test/resources/isbn-ranges-wrong-range.xml" to "Malformed range '6499999-6000000' at prefix '978'",
            "src/test/resources/isbn-ranges-malformed-number.xml" to "Malformed range '0000000-19x9999' at prefix '978-0'",
            "src/test/resources/isbn-ranges-incomplete-group.xml" to "Can't parse incomplete group at prefix '979'",
            "src/test/resources/isbn-ranges-incomplete-range.xml" to "Can't parse incomplete rule at prefix '978-0'",
        ).forEach { (fileName, expectedErrorMessage) ->
            assertThrows<Reader.ReadException> {
                FileInputStream(fileName).use {
                    Reader.read(it)
                }
            }.let {
                assertEquals(expectedErrorMessage, it.message)
            }
        }
    }
}