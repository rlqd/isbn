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

import org.jetbrains.annotations.ApiStatus

/**
 * Object containing helper functions for calculating and validating check digit for various versions of ISBN.
 * Note that ISBNs must be prepared (any separators removed) before using them here.
 */
@ApiStatus.Internal
internal object CheckDigit {
    /**
     * Calculates ISBN check digit (checksum)
     * Input may be passed with or without a check digit, it will be stripped to 9 (ISBN-10) or 12 (ISBN-13) characters long.
     *
     * @see calculateForGTIN() for different formats
     *
     * @param isbn book number without any separators
     * @param isShort is input should be treated as ISBN-10
     * @return digit character (or sometimes 'X' for ISBN-10 format)
     *
     * @throws IllegalArgumentException if ISBN string length is too small or contains non-digit characters
     */
    @Throws(IllegalArgumentException::class)
    fun calculate(isbn: String, isShort: Boolean = isbn.length <= 10): Char
        = if (isShort) calculateForISBN10(isbn.take(9)) else calculateForGTIN(isbn.take(12))

    /**
     * Compares supplied check digit to a calculated one.
     *
     * @param isbn book number without any separators
     * @param checkDigit supply check digit separately if it's missing from the supplied isbn
     * @param isShort if ISBN10 format should be used
     * @return (Boolean) is supplied check digit valid (same as calculated by supplied ISBN)
     *
     * @throws IllegalArgumentException if ISBN string length is too small or contains non-digit characters
     */
    @Throws(IllegalArgumentException::class)
    fun compare(isbn: String, checkDigit: Char = checkDigit(isbn), isShort: Boolean = isbn.length <= 10)
        = calculate(isbn, isShort) == checkDigit

    /**
     * Compares supplied check digit to a calculated one.
     * This is a separate function for GTIN-14 codes.
     *
     * @param code code string without any separators
     * @param checkDigit supply check digit separately if it's missing from the supplied code
     * @return (Boolean) is supplied check digit valid (same as calculated by supplied code)
     *
     * @throws IllegalArgumentException if input string length is too small or contains non-digit characters
     */
    @Throws(IllegalArgumentException::class)
    fun compareGTIN14(code: String, checkDigit: Char = checkDigit(code, 13)): Boolean
        = calculateForGTIN(code.take(13)) == checkDigit

    /**
     * Checks if supplied check digit is valid and throws ISBNIntegrityException if it's not.
     *
     * @param isbn book number without any separators
     * @param checkDigit supply check digit separately if it's missing from the supplied isbn
     * @param isShort if ISBN10 format should be used
     *
     * @throws ISBNIntegrityException if supplied check digit mismatch calculated by supplied ISBN
     * @throws IllegalArgumentException if ISBN string length is too small or contains non-digit characters
     */
    @Throws(ISBNIntegrityException::class, IllegalArgumentException::class)
    fun assert(isbn: String, checkDigit: Char = checkDigit(isbn), isShort: Boolean = isbn.length <= 10) {
        if (!compare(isbn, checkDigit, isShort)) {
            throw ISBNIntegrityException("Supplied check digit for ${if (isShort) "ISBN-10" else "ISBN-13"} doesn't match calculated value", 1)
        }
    }

    /**
     * Checks if supplied check digit is valid and throws ISBNIntegrityException if it's not.
     * This is a separate function for GTIN-14 codes.
     *
     * @param code code string without any separators
     * @param checkDigit supply check digit separately if it's missing from the supplied code
     *
     * @throws IllegalArgumentException if input string length is too small or contains non-digit characters
     */
    @Throws(ISBNIntegrityException::class, IllegalArgumentException::class)
    fun assertGTIN14(code: String, checkDigit: Char = checkDigit(code, 13)) {
        if (!compareGTIN14(code, checkDigit)) {
            throw ISBNIntegrityException("Supplied check digit for GTIN-14 doesn't match calculated value", 1)
        }
    }

    /**
     * Consider using calculate() instead, unless you stripped the input manually and know what you're doing.
     *
     * @param isbn book number without any separators exactly 9 characters long
     * @throws IllegalArgumentException if ISBN string length is wrong or contains non-digit characters
     */
    @Throws(IllegalArgumentException::class)
    fun calculateForISBN10(isbn: String): Char {
        if (isbn.length != 9) {
            throw IllegalArgumentException("Expected input exactly 9 characters long for ISBN-10")
        }

        val checksum = (11 - isbn.mapIndexed { i, c ->
            c.digitToInt() * (10 - i)
        }.sum() % 11) % 11

        return if (checksum == 10) 'X' else checksum.digitToChar()
    }

    /**
     * Consider using calculate() instead, unless you stripped the input manually and know what you're doing.
     * This method may be used directly if you need to calculate for different formats (e.g. GTIN-14).
     *
     * @param code book number without any separators at least 12 characters long
     * @throws IllegalArgumentException if ISBN string length is too small or contains non-digit characters
     */
    @Throws(IllegalArgumentException::class)
    fun calculateForGTIN(code: String): Char {
        if (code.length < 12) {
            throw IllegalArgumentException("Expected input at least 12 characters long for ISBN-13 or GTIN-14")
        }

        var weight = 1
        val checksum = (10 - code.reversed().map {
            weight = 4 - weight
            it.digitToInt() * weight
        }.sum() % 10) % 10

        return checksum.digitToChar()
    }

    @Throws(IllegalArgumentException::class)
    private fun checkDigit(code: String, index: Int = if (code.length > 10) 12 else 9): Char {
        if (code.length <= index) {
            throw IllegalArgumentException("Got code without check digit (i=$index) to compare (${code.length} chars long)")
        }
        return code[index]
    }
}