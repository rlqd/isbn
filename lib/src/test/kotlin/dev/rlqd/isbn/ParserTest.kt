package dev.rlqd.isbn

import dev.rlqd.isbn.ranges.Provider
import dev.rlqd.isbn.ranges.Range
import dev.rlqd.isbn.ranges.RangeGroup
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

class ParserTest {
    @Test
    fun testFormatDetection() {
        val parser = Parser(MockRangesProvider())

        mapOf(
            "978-5-17-095179-6" to BookNumber.Type.ISBN_13,
            "978 5 17 095179 6" to BookNumber.Type.ISBN_13,
            "978-5-17-095179" to BookNumber.Type.ISBN_13,
            "9785170951796" to BookNumber.Type.EAN_13,
            "978517095179" to BookNumber.Type.EAN_13,
            "10.978.517/0951796" to BookNumber.Type.ISBN_A,
            "1-55404-295-3" to BookNumber.Type.ISBN_10,
            "1 55404 295 3" to BookNumber.Type.ISBN_10,
            "1-55404-295" to BookNumber.Type.ISBN_10,
            "1554042953" to BookNumber.Type.EAN_10,
            "155404295" to BookNumber.Type.EAN_10,
            "09785170951796" to BookNumber.Type.GTIN_14,
            "19785170951793" to BookNumber.Type.GTIN_14,
            "979-0-2600-0043-8" to BookNumber.Type.ISMN,
            "M-2600-0043-8" to BookNumber.Type.ISMN,
            "9790260000438" to BookNumber.Type.MUSIC_EAN,
        ).forEach { (code, format) ->
            val bn = parser.parse(code, false)
            assertEquals(format, bn.metadata.type, "Detected wrong format for $code (must be ${format.printedName})")
        }
    }

    @Test
    fun testSeparatorDetection() {
        val parser = Parser(MockRangesProvider())

        mapOf(
            "978-5-17-095179-6" to '-',
            "978 5 17 095179 6" to ' ',
            "978-5-17-095179" to '-',
            "979-0-2600-0043-8" to '-',
            "M-2600-0043-8" to '-',
            "9785170951796" to null,
            "10.978.517/0951796" to null,
            "1-55404-295-3" to '-',
            "1 55404 295 3" to ' ',
            "1-55404-295" to '-',
            "1554042953" to null,
            "09785170951796" to null,
        ).forEach { (code, separator) ->
            val bn = parser.parse(code, false)
            assertEquals(separator, bn.metadata.separator, "Detected wrong separator for $code (must be ${separator ?: "null"}")
        }
    }

    @Test
    fun testSourceFormatting() {
        val parser = Parser(MockRangesProvider())

        mapOf(
            "978-5-17-095179-6" to "978-5-17-095179-6",
            "978 5 17 095179 6" to "978 5 17 095179 6",
            "978-5-17-095179"   to "978-5-17-095179-6",
            "9785170951796"     to "9785170951796",
            "978517095179"      to "9785170951796",
            "979-0-2600-0043-8" to "979-0-2600-0043-8",
            "M-2600-0043-8"     to "979-0-2600-0043-8",
            "9790260000438"     to "9790260000438",
            "1-55404-295-X"     to "1-55404-295-X",
            "1 55404 295 X"     to "1 55404 295 X",
            "1-55404-295"       to "1-55404-295-X",
            "155404295X"        to "155404295X",
            "155404295"         to "155404295X",
            "09785170951796"    to "09785170951796",
            "19785170951793"    to "19785170951793",
            "10.978.517/0951796" to "10.978.517/0951796",
        ).forEach { (inputCode, standardCode) ->
            val bn = parser.parse(inputCode, false)
            assertEquals(standardCode, bn.toSourceFormat(), "Wrong source formatting of $inputCode (must be $standardCode)")
        }
    }

    @Test
    fun testParseSuccess() {
        val parser = Parser(MockRangesProvider())

        // Test in different formats
        listOf(
            "978-5-17-095179-6",
            "978 5 17 095179 6",
            "9785170951796",
            "10.978.517/0951796",
        ).forEach { code ->
            val isbn = parser.parse(code)
            // Test all elements
            assertEquals(978u, isbn.gs1, code)
            assertEquals("978", isbn.gs1Element, code)
            assertEquals(5u, isbn.group, code)
            assertEquals("5", isbn.groupElement, code)
            assertEquals(17u, isbn.registrant, code)
            assertEquals("17", isbn.registrantElement, code)
            assertEquals(95179u, isbn.publication, code)
            assertEquals("095179", isbn.publicationElement, code)
            // Test metadata
            assertFalse(isbn.metadata.type.isShort, code)
            assertTrue(isbn.metadata.hasCheckDigit, code)
            assertTrue(isbn.metadata.isCheckDigitValid, code)
            assertEquals('6', isbn.metadata.checkDigit, code)
            assertEquals(1, isbn.metadata.groupLength, code)
            assertEquals(2, isbn.metadata.registrantLength, code)
            assertEquals(6, isbn.metadata.publicationLength, code)
            assertEquals("former U.S.S.R", isbn.metadata.agencyName, code)
            assertNull(isbn.metadata.packagingIndicator)
        }

        // Test when registrant starts with 0
        val isbn0 = parser.parse("978-5-04-097876-2")
        assertEquals(978u, isbn0.gs1)
        assertEquals("978", isbn0.gs1Element)
        assertEquals(5u, isbn0.group)
        assertEquals("5", isbn0.groupElement)
        assertEquals(4u, isbn0.registrant)
        assertEquals("04", isbn0.registrantElement)
        assertEquals(97876u, isbn0.publication)
        assertEquals("097876", isbn0.publicationElement)
        assertEquals(1, isbn0.metadata.groupLength)
        assertEquals(2, isbn0.metadata.registrantLength)
        assertEquals(6, isbn0.metadata.publicationLength)

        // Test ISMN
        listOf(
            "979-0-66003-838-3",
            "9790660038383",
            "M-66003-838-3",
        ).forEach { code ->
            val ismn = parser.parse(code)
            assertEquals(979u, ismn.gs1, code)
            assertEquals("979", ismn.gs1Element, code)
            assertEquals(0u, ismn.group, code)
            assertEquals("0", ismn.groupElement, code)
            assertEquals(66003u, ismn.registrant, code)
            assertEquals("66003", ismn.registrantElement, code)
            assertEquals(838u, ismn.publication, code)
            assertEquals("838", ismn.publicationElement, code)
            assertFalse(ismn.metadata.type.isShort, code)
            assertTrue(ismn.metadata.hasCheckDigit, code)
            assertTrue(ismn.metadata.isCheckDigitValid, code)
            assertEquals('3', ismn.metadata.checkDigit, code)
            assertEquals("Musicland", ismn.metadata.agencyName, code)
            assertNull(ismn.metadata.packagingIndicator, code)
        }
    }

    @Test
    fun testParseWrongOrMissingChecksum() {
        val parser = Parser(MockRangesProvider())
        mapOf(
            "978-5-17-095179-2" to true,
            "978-5-17-095179" to false,
            "9785170951792" to true,
            "978517095179" to false,
            "1-55404-295-3" to true,
            "1-55404-295" to false,
            "979-0-66003-838-4" to true,
            "979-0-66003-838" to false,
        ).forEach { (code, expectedToHave) ->
            val isbn = parser.parse(code, false)
            assertEquals(expectedToHave, isbn.metadata.hasCheckDigit, code)
            assertFalse(isbn.metadata.isCheckDigitValid, code)
        }
    }

    @Test
    fun testParseXCheckDigit() {
        val parser = Parser(MockRangesProvider())
        val isbn = parser.parse("1-55404-295-X")
        assertTrue(isbn.metadata.type.isShort)
        assertTrue(isbn.metadata.hasCheckDigit)
        assertEquals('X', isbn.metadata.checkDigit)
        assertTrue(isbn.metadata.isCheckDigitValid)
    }

    @Test
    fun testParseGTIN14() {
        val parser = Parser(MockRangesProvider())
        mapOf(
            "09785170951796" to Pair(0,'6'),
            "19785170951793" to Pair(1,'3'),
        ).forEach { (code, expectedData) ->
            val (indicator, checkDigit) = expectedData
            val isbn = parser.parse(code)
            // Test all elements
            assertEquals(978u, isbn.gs1, code)
            assertEquals("978", isbn.gs1Element, code)
            assertEquals(5u, isbn.group, code)
            assertEquals("5", isbn.groupElement, code)
            assertEquals(17u, isbn.registrant, code)
            assertEquals("17", isbn.registrantElement, code)
            assertEquals(95179u, isbn.publication, code)
            assertEquals("095179", isbn.publicationElement, code)
            // Test metadata
            assertEquals(BookNumber.Type.GTIN_14, isbn.metadata.type)
            assertTrue(isbn.metadata.hasCheckDigit, code)
            assertTrue(isbn.metadata.isCheckDigitValid, code)
            assertEquals(checkDigit, isbn.metadata.checkDigit, code)
            assertEquals(1, isbn.metadata.groupLength, code)
            assertEquals(2, isbn.metadata.registrantLength, code)
            assertEquals(6, isbn.metadata.publicationLength, code)
            assertEquals("former U.S.S.R", isbn.metadata.agencyName, code)
            assertEquals(indicator, isbn.metadata.packagingIndicator, code)
        }
    }

    @Test
    fun testParseErrors() {
        val parser = Parser(MockRangesProvider())
        mapOf(
            " " to Pair("1-1", "No code provided at > <"),
            "9@85170951796" to Pair("1-2", "Unexpected characters in the code at >9@8<5170951796"),
            "978@170951796" to Pair("1-2", "Unexpected characters in the code at ...>@170951<796"),
            "97851709517@6" to Pair("1-2", "Unexpected characters in the code at ...>09517@<6"),
            "978517095179@" to Pair("1-2", "Unexpected characters in the code at 978517095179>@< (EAN-13)"),
            "99785170951796" to Pair("1-2", "Unexpected characters in the code at >9<9785170951796 (GTIN-14)"), // wrong packaging indicator (must be 0-8)
            "12345" to Pair("1-3", "Code length is not matching any known type at >12345<"),
            "9775170951796" to Pair("1-4", "GS1 element is unknown at >977<5170951796"),
            "9783170951796" to Pair("1-5", "Group element is unknown at >978-3<170951796"),
            "9785200951796" to Pair("1-6", "Failed to find any matching ISBN range at ...>2009517<96"),
            "9785000951796" to Pair("1-7", "Found matching ISBN range with 0 length at ...>0009517<96"),
        ).forEach { (code, errorData) ->
            val (errorCode, errorMessage) = errorData
            assertThrows<ISBNParseException> {
                parser.parse(code, false)
            }.let {
                assertEquals(errorMessage, it.message)
                assertEquals(errorCode, it.errorCode)
            }
        }
    }

    @Test
    fun testIntegrityErrors() {
        val parser = Parser(MockRangesProvider())
        mapOf(
            "9785370951796" to "2-1", // damaged code (check digit mismatch)
            "978517095179" to "2-2",  // missing check digit
            "9785X70951796" to "2-3", // invalid characters in the code
        ).forEach { (isbn, errorCode) ->
            assertThrows<ISBNIntegrityException> {
                parser.parse(isbn)
            }.let {
                assertEquals(errorCode, it.errorCode)
            }
        }
    }

    @Test
    fun testISMNRanges() {
        val parser = Parser(MockRangesProvider())
        mapOf(
            "979-0-000-12345-8" to 5,
            "979-0-003-12345-5" to 5,
            "979-0-015-12345-0" to 5,
            "979-0-2600-0043-8" to 4,
            "979-0-66003-838-3" to 3,
            "979-0-802301-10-8" to 2,
            "979-0-9018302-1-9" to 1,
        ).forEach { (code, expectedPublicationLength) ->
            val bn = parser.parse(code)
            assertEquals(BookNumber.Type.ISMN, bn.metadata.type, code)
            assertEquals(1, bn.metadata.groupLength, code)
            assertEquals(expectedPublicationLength, bn.metadata.publicationLength, code)
            assertEquals(8 - expectedPublicationLength, bn.metadata.registrantLength, code)
            assertEquals(code, bn.toSourceFormat())
        }
    }
}

class MockRangesProvider: Provider {
    private val map = mapOf(
        "978" to RangeGroup(
            "International ISBN Agency",
            listOf(
                Range(0,5999999,1),
            ),
        ),
        "978-1" to RangeGroup(
            "English language",
            listOf(
                Range(5500000,6499999,5),
            ),
        ),
        "978-5" to RangeGroup(
            "former U.S.S.R",
            listOf(
                Range(0,99999,0),
                Range(100000,1999999,2),
            ),
        ),
    )

    override fun getRanges(prefix: String): RangeGroup? = map[prefix]
}