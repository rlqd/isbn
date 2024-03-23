package dev.rlqd.isbn

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckDigitTest {
    @Test
    fun calculate() {
        mapOf(
                // ISBN-10
                "000000000" to '0',
                "019853453" to '1',
                "0198534531" to '1',
                "0198534532" to '1',
                "155404295" to 'X',
                // ISBN-13
                "000000000000" to '0',
                "978517983355" to '0',
                "9785171136161" to '1',
                "9785171017290" to '3',
                "978517108312" to '0',
                "978517091508" to '8',
        ).forEach { (isbn, checkDigit) ->
            assertEquals(checkDigit, CheckDigit.calculate(isbn), "Wrong check digit calculated for input $isbn")
        }
    }

    @Test
    fun calculateForGTIN() {
        mapOf(
                // ISBN-13
                "978517983355" to '0',
                "978517113616" to '1',
                "978517091508" to '8',
                // GTIN-14
                "0978517091508" to '8',
                "1978517091508" to '5',
        ).forEach { (code, checkDigit) ->
            assertEquals(checkDigit, CheckDigit.calculateForGTIN(code), "Wrong check digit calculated for input $code")
        }
    }

    @Test
    fun compare() {
        mapOf(
                // ISBN-10
                "0000000000" to true,
                "0198534531" to true,
                "155404295X" to true,
                "0198534532" to false,
                // ISBN-13
                "9785179833550" to true,
                "9785171136161" to true,
                "9785170915088" to true,
                "9785170915083" to false,
        ).forEach { (isbn, valid) ->
            assertEquals(valid, CheckDigit.compare(isbn), "Wrong check digit compare result for input $isbn")
        }
    }

    @Test
    fun compareGTIN14() {
        mapOf(
                "09785170915088" to true,
                "19785170915085" to true,
                "19785170915081" to false,
        ).forEach { (code, valid) ->
            assertEquals(valid, CheckDigit.compareGTIN14(code), "Wrong check compare result for input $code")
        }
    }

    @Test
    fun assert() {
        mapOf(
                "0198534532" to "Supplied check digit for ISBN-10 doesn't match calculated value",
                "9785170915083" to "Supplied check digit for ISBN-13 doesn't match calculated value",
        ).forEach { (isbn, msg) ->
            assertThrows<ISBNIntegrityException> {
                CheckDigit.assert(isbn)
            }.let {
                assertEquals(msg, it.message)
            }
        }
        listOf(
                "0198534531",
                "9785170915088",
        ).forEach { isbn ->
            assertDoesNotThrow {
                CheckDigit.assert(isbn)
            }
        }
    }

    @Test
    fun assertGTIN14() {
        mapOf(
                "09785170915084" to "Supplied check digit for GTIN-14 doesn't match calculated value",
                "19785170915081" to "Supplied check digit for GTIN-14 doesn't match calculated value",
        ).forEach { (code, msg) ->
            assertThrows<ISBNIntegrityException> {
                CheckDigit.assertGTIN14(code)
            }.let {
                assertEquals(msg, it.message)
            }
        }
        listOf(
                "09785170915088",
                "19785170915085",
        ).forEach { code ->
            assertDoesNotThrow {
                CheckDigit.assertGTIN14(code)
            }
        }
    }
}