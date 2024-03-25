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

package dev.rlqd.isbn

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.*

/**
 * "E2E" tests to assert library usability with the default provider and pre-built ranges.
 * Configured to run against the output jar.
 */
class ISBNTest {
    @Test
    fun parse() {
        val bn = ISBN.parse("978-5-17-095179-6")
        assertEquals(978u, bn.gs1)
        assertEquals(5u, bn.group)
        assertEquals(17u, bn.registrant)
        assertEquals(95179u, bn.publication)
        assertEquals('6', bn.metadata.checkDigit)
        assertEquals(BookNumber.Type.ISBN_13, bn.metadata.type)
        assertEquals('-', bn.metadata.separator)
        assertNull(bn.metadata.packagingIndicator)
    }

    @Test
    fun convertToISBN13() {
        assertEquals(
            "978-5-17-095179-6",
            ISBN.convertToISBN13("978-5-17-095179-6"),
        )
        assertEquals(
            "978 5 17 095179 6",
            ISBN.convertToISBN13("978-5-17-095179-6", ' '),
        )

        assertThrows<ISBNConvertException> {
            ISBN.convertToISBN10("979-0-2600-0043-8") // ISMN
        }.let {
            assertEquals("4-3", it.errorCode)
        }
    }

    @Test
    fun convertToISBN10() {
        assertEquals(
            "5-17-095179-5",
            ISBN.convertToISBN10("978-5-17-095179-6"),
        )
        assertEquals(
            "5 17 095179 5",
            ISBN.convertToISBN10("978-5-17-095179-6", ' '),
        )

        assertThrows<ISBNConvertException> {
            ISBN.convertToISBN10("979-10-90636-07-1")
        }.let {
            assertEquals("4-1", it.errorCode)
        }
        assertThrows<ISBNConvertException> {
            ISBN.convertToISBN10("979-0-2600-0043-8") // ISMN
        }.let {
            assertEquals("4-3", it.errorCode)
        }
    }

    @Test
    fun convertToEAN13() {
        assertEquals(
            "9785170951796",
            ISBN.convertToEAN13("978-5-17-095179-6"),
        )

        assertThrows<ISBNConvertException> {
            ISBN.convertToEAN13("9790260000438") // ISMN
        }.let {
            assertEquals("4-3", it.errorCode)
        }
    }

    @Test
    fun convertToEAN10() {
        assertEquals(
            "5170951795",
            ISBN.convertToEAN10("978-5-17-095179-6"),
        )

        assertThrows<ISBNConvertException> {
            ISBN.convertToEAN10("979-10-90636-07-1")
        }.let {
            assertEquals("4-1", it.errorCode)
        }
        assertThrows<ISBNConvertException> {
            ISBN.convertToEAN10("979-0-2600-0043-8") // ISMN
        }.let {
            assertEquals("4-3", it.errorCode)
        }
    }

    @Test
    fun convertToGTIN14() {
        assertEquals(
            "09785170951796",
            ISBN.convertToGTIN14("978-5-17-095179-6", 0),
        )
        assertEquals(
            "19785170951793", // note check digit changed
            ISBN.convertToGTIN14("978-5-17-095179-6", 1),
        )

        assertThrows<ISBNConvertException> {
            ISBN.convertToGTIN14("978-5-17-095179-6")
        }.let {
            assertEquals("4-2", it.errorCode)
        }
        assertThrows<ISBNConvertException> {
            ISBN.convertToGTIN14("979-0-2600-0043-8") // ISMN
        }.let {
            assertEquals("4-3", it.errorCode)
        }
    }

    @Test
    fun convertToISBNA() {
        assertEquals(
            "10.978.517/0951796",
            ISBN.convertToISBNA("978-5-17-095179-6"),
        )
        assertThrows<ISBNConvertException> {
            ISBN.convertToISBNA("979-0-2600-0043-8") // ISMN
        }.let {
            assertEquals("4-3", it.errorCode)
        }
    }

    @Test
    fun convertToISMN() {
        assertEquals(
            "979-0-2600-0043-8",
            ISBN.convertToISMN("9790260000438"),
        )
        assertThrows<ISBNConvertException> {
            ISBN.convertToISMN("978-5-17-095179-6") // Non-music
        }.let {
            assertEquals("4-3", it.errorCode)
        }
    }

    @Test
    fun convertToMusicEAN() {
        assertEquals(
            "9790260000438",
            ISBN.convertToMusicEAN("979-0-2600-0043-8"),
        )
        assertThrows<ISBNConvertException> {
            ISBN.convertToMusicEAN("978-5-17-095179-6") // Non-music
        }.let {
            assertEquals("4-3", it.errorCode)
        }
    }

    @Test
    fun validateAsISBN13() {
        assertDoesNotThrow {
            ISBN.validateAsISBN13("978-5-17-095179-6")
            ISBN.validateAsISBN13("978 5 17 095179 6")
            ISBN.validateAsISBN13("979-10-90636-07-1")
            ISBN.validateAsType("978-5-17-095179-6", BookNumber.Type.ISBN_13)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsISBN13("978-5-1709-5179-6") // misplaced separator
        }.let {
            assertEquals("3-1", it.errorCode)
            assertEquals("'978-5-1709-5179-6' is not a valid ISBN-13, expected '978-5-17-095179-6'", it.message)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsISBN13("978 5 1709 5179 6")
        }.let {
            assertEquals("3-1", it.errorCode)
            assertEquals("'978 5 1709 5179 6' is not a valid ISBN-13, expected '978 5 17 095179 6'", it.message)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsISBN13("979-0-2600-0043-8") // ISMN
        }.let {
            assertEquals("3-2", it.errorCode)
            assertEquals("'979-0-2600-0043-8' is not a valid ISBN-13, detected ISMN instead", it.message)
        }
    }

    @Test
    fun validateAsISBN10() {
        assertDoesNotThrow {
            ISBN.validateAsISBN10("5-17-095179-5")
            ISBN.validateAsISBN10("5 17 095179 5")
            ISBN.validateAsType("5-17-095179-5", BookNumber.Type.ISBN_10)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsISBN10("5-17_095179-5") // wrong separator
        }.let {
            assertEquals("3-1", it.errorCode)
            assertEquals("'5-17_095179-5' is not a valid ISBN-10, expected '5-17-095179-5'", it.message)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsISBN10("979-10-90636-07-1")
        }.let {
            assertEquals("3-2", it.errorCode)
            assertEquals("'979-10-90636-07-1' is not a valid ISBN-10, detected ISBN-13 instead", it.message)
        }
    }

    @Test
    fun validateAsEAN13() {
        assertDoesNotThrow {
            ISBN.validateAsEAN13("9785170951796")
            ISBN.validateAsType("9785170951796", BookNumber.Type.EAN_13)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsEAN13("978-5-17-095179-6") // must be without separators
        }.let {
            assertEquals("3-2", it.errorCode)
            assertEquals("'978-5-17-095179-6' is not a valid EAN-13, detected ISBN-13 instead", it.message)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsEAN13("9790260000438") // ISMN
        }.let {
            assertEquals("3-2", it.errorCode)
            assertEquals("'9790260000438' is not a valid EAN-13, detected EAN-13/ISMN instead", it.message)
        }
    }

    @Test
    fun validateAsEAN10() {
        assertDoesNotThrow {
            ISBN.validateAsEAN10("5170951795")
            ISBN.validateAsType("5170951795", BookNumber.Type.EAN_10)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsEAN10("5-17-095179-5") // must be without separators
        }.let {
            assertEquals("3-2", it.errorCode)
            assertEquals("'5-17-095179-5' is not a valid EAN-10, detected ISBN-10 instead", it.message)
        }
    }

    @Test
    fun validateAsGTIN14() {
        assertDoesNotThrow {
            ISBN.validateAsGTIN14("09785170951796")
            ISBN.validateAsGTIN14("19785170951793")
            ISBN.validateAsType("19785170951793", BookNumber.Type.GTIN_14)
        }
    }

    @Test
    fun validateAsISBNA() {
        assertDoesNotThrow {
            ISBN.validateAsISBNA("10.978.517/0951796")
            ISBN.validateAsType("10.978.517/0951796", BookNumber.Type.ISBN_A)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsISBNA("10.978.517.0951796") // wrong separator
        }.let {
            assertEquals("3-1", it.errorCode)
            assertEquals("'10.978.517.0951796' is not a valid ISBN-A, expected '10.978.517/0951796'", it.message)
        }
    }

    @Test
    fun validateAsISMN() {
        assertDoesNotThrow {
            ISBN.validateAsISMN("979-0-2600-0043-8")
            ISBN.validateAsType("979-0-2600-0043-8", BookNumber.Type.ISMN)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsISMN("9790260000438")
        }.let {
            assertEquals("3-2", it.errorCode)
            assertEquals("'9790260000438' is not a valid ISMN, detected EAN-13/ISMN instead", it.message)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsISMN("978-5-17-095179-6") // Non-music
        }.let {
            assertEquals("3-2", it.errorCode)
            assertEquals("'978-5-17-095179-6' is not a valid ISMN, detected ISBN-13 instead", it.message)
        }
    }

    @Test
    fun validateAsMusicEAN() {
        assertDoesNotThrow {
            ISBN.validateAsMusicEAN("9790260000438")
            ISBN.validateAsType("9790260000438", BookNumber.Type.MUSIC_EAN)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsMusicEAN("979-0-2600-0043-8")
        }.let {
            assertEquals("3-2", it.errorCode)
            assertEquals("'979-0-2600-0043-8' is not a valid EAN-13/ISMN, detected ISMN instead", it.message)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsMusicEAN("9785170951796") // Non-music
        }.let {
            assertEquals("3-2", it.errorCode)
            assertEquals("'9785170951796' is not a valid EAN-13/ISMN, detected EAN-13 instead", it.message)
        }
    }

    @Test
    fun validateAsAny() {
        assertDoesNotThrow {
            listOf(
                "978-5-17-095179-6",
                "978 5 17 095179 6",
                "5-17-095179-5",
                "5 17 095179 5",
                "9785170951796",
                "5170951795",
                "09785170951796",
                "19785170951793",
                "10.978.517/0951796",
                "979-0-2600-0043-8",
                "9790260000438",
            ).forEach(ISBN::validateAsAny)
        }
        assertThrows<ISBNValidateException> {
            ISBN.validateAsAny("978-5-1709-5179-6") // misplaced separator
        }.let {
            assertEquals("3-1", it.errorCode)
            assertEquals("'978-5-1709-5179-6' is detected to be ISBN-13, but incorrectly formatted. Expected: '978-5-17-095179-6'", it.message)
        }
    }
}