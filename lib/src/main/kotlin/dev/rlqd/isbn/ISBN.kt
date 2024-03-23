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

import dev.rlqd.isbn.ranges.DefaultProvider
import dev.rlqd.isbn.ranges.Provider
import org.jetbrains.annotations.ApiStatus

/**
 * Main library class for parsing, validating and converting International Standard Book Numbers (ISBN).
 *
 * Supports the following formats:
 *
 * ISBN-13 or EAN-13, with or without check digit. Examples:
 * 978-92-95055-12-4
 * 978 92 95055 12 4
 * 9789295055124
 * 978929505512
 *
 * ISBN-10 or EAN-10, with or without check digit. Examples:
 * 92-95055-12-8
 * 92 95055 12 8
 * 9295055128
 * 929505512
 *
 * Only properly formatted ISBN-A. Example:
 * 10.978.12345/99990
 *
 * Only full 14-digits GTIN-14. Example:
 * 09789295055124
 */
@ApiStatus.AvailableSince("1.0.0")
sealed class ISBN(
    rangesProvider: Provider,
) {
    companion object Default: ISBN(DefaultProvider())
    class Custom(rangesProvider: Provider): ISBN(rangesProvider)

    private val parser = Parser(rangesProvider)

    /**
     * @param input ISBN in one of supported formats (see class description)
     * @param checkIntegrity throw exception if check digit is missing or incorrect (useful for validating scanned/user inputs)
     * @return an object, containing book number information, and which can be converted to any supported format string
     *
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if integrity check is enabled and input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun parse(input: String, checkIntegrity: Boolean = true): BookNumber
        = parser.parse(input, checkIntegrity)

    /**
     * @param input ISBN in one of supported formats (see class description)
     * @return string correctly formatted as ISBN-13 (example: 978-92-95055-12-4)
     *
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun convertToISBN13(input: String, separator: Char = '-'): String
        = parse(input).toISBN13(separator)

    /**
     * @param input ISBN in one of supported formats (see class description)
     * @return string correctly formatted as ISBN-10 (example: 92-95055-12-8)
     *
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     * @throws ISBNConvertException if input can't be converted, because it uses new gs1 value
     */
    @Throws(ISBNException::class)
    fun convertToISBN10(input: String, separator: Char = '-'): String
        = parse(input).toISBN10(separator)

    /**
     * @param input ISBN in one of supported formats (see class description)
     * @return string correctly formatted as EAN-13 (example: 9789295055124)
     *
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun convertToEAN13(input: String): String
        = parse(input).toEAN13()

    /**
     * @param input ISBN in one of supported formats (see class description)
     * @return string correctly formatted as EAN-10 (example: 9295055128)
     *
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     * @throws ISBNConvertException if input can't be converted, because it uses new gs1 value
     */
    @Throws(ISBNException::class)
    fun convertToEAN10(input: String): String
        = parse(input).toEAN10()

    /**
     * @param input ISBN in one of supported formats (see class description)
     * @return string correctly formatted as ISBN-A (example: 10.978.12345/99990)
     *
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun convertToISBNA(input: String): String
        = parse(input).toISBNA()

    /**
     * @param input ISBN in one of supported formats (see class description)
     * @return string correctly formatted as GTIN-14 (example: 09789295055124)
     *
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     * @throws ISBNConvertException if valid packaging indicator is missing in the input and not supplied separately
     */
    @Throws(ISBNException::class)
    fun convertToGTIN14(input: String, indicator: Int? = null): String
        = parse(input).let {
            if (indicator == null) it.toGTIN14() else it.toGTIN14(indicator)
        }

    /**
     * Validates that input is a correctly formatted ISBN-13.
     *
     * @param input ISBN in one of supported formats (see class description)
     *
     * @throws ISBNValidateException if input is formatted incorrectly
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun validateAsISBN13(input: String)
        = validateAsFormat(input, BookNumber.Format.ISBN_13)

    /**
     * Validates that input is a correctly formatted ISBN-10.
     *
     * @param input ISBN in one of supported formats (see class description)
     *
     * @throws ISBNValidateException if input is formatted incorrectly
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun validateAsISBN10(input: String)
        = validateAsFormat(input, BookNumber.Format.ISBN_10)

    /**
     * Validates that input is a correctly formatted EAN-13.
     *
     * @param input ISBN in one of supported formats (see class description)
     *
     * @throws ISBNValidateException if input is formatted incorrectly
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun validateAsEAN13(input: String)
        = validateAsFormat(input, BookNumber.Format.EAN_13)

    /**
     * Validates that input is a correctly formatted EAN-10.
     *
     * @param input ISBN in one of supported formats (see class description)
     *
     * @throws ISBNValidateException if input is formatted incorrectly
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun validateAsEAN10(input: String)
        = validateAsFormat(input, BookNumber.Format.EAN_10)

    /**
     * Validates that input is a correctly formatted ISBN-A.
     *
     * @param input ISBN in one of supported formats (see class description)
     *
     * @throws ISBNValidateException if input is formatted incorrectly
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun validateAsISBNA(input: String)
        = validateAsFormat(input, BookNumber.Format.ISBN_A)

    /**
     * Validates that input is a correctly formatted GTIN-14.
     *
     * @param input ISBN in one of supported formats (see class description)
     *
     * @throws ISBNValidateException if input is formatted incorrectly
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun validateAsGTIN14(input: String)
        = validateAsFormat(input, BookNumber.Format.GTIN_14)

    /**
     * Validates that input is a correctly formatted code of a given format.
     *
     * @param input ISBN in one of supported formats (see class description)
     *
     * @throws ISBNValidateException if input is formatted incorrectly
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun validateAsFormat(input: String, format: BookNumber.Format): BookNumber {
        val bn = parse(input)
        val converted = try {
            bn.toFormat(format, true)
        } catch (e: ISBNConvertException) {
            throw ISBNValidateException("'$input' is not a valid ${format.printedName}, as it can't be converted to it", 2, e)
        }
        if (input != converted) {
            throw ISBNValidateException("'$input' is not a valid ${format.printedName}, expected '$converted'", 1)
        }
        return bn
    }

    /**
     * Validates that input is a correctly formatted code of any supported format.
     * It does that by attempting to detect the format and then validating that the input is correctly formatted.
     *
     * @param input ISBN in one of supported formats (see class description)
     *
     * @throws ISBNValidateException if input is formatted incorrectly
     * @throws ISBNParseException if failed to parse input
     * @throws ISBNIntegrityException if input has incorrect or missing check digit
     */
    @Throws(ISBNException::class)
    fun validateAsAny(input: String): BookNumber {
        val bn = parse(input)
        val converted = bn.toSourceFormat()
        if (input != converted) {
            throw ISBNValidateException("'$input' is detected to be ${bn.metadata.format.printedName}, but incorrectly formatted. Expected: '$converted'", 1)
        }
        return bn
    }
}