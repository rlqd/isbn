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
 * International Standard Book Number (BookNumber for short).
 * Contains elements as defined by "ISBN International Users Manual 7th edition" on page 11.
 */
@ApiStatus.AvailableSince("1.0.0")
data class BookNumber internal constructor(
    val gs1: UShort,
    val group: UInt,
    val registrant: UInt,
    val publication: UInt,
    val metadata: Metadata,
) {
    internal constructor(): this(
        0u, 0u, 0u, 0u,
        Metadata("", Format.ISBN_13, null, null, null, 0, 0, ""),
    )

    enum class Format(val printedName: String, val isShort: Boolean = false) {
        ISBN_13("ISBN-13"),
        ISBN_10("ISBN-10", true),
        EAN_13("EAN-13"),
        EAN_10("EAN-10", true),
        ISBN_A("ISBN-A"),
        GTIN_14("GTIN-14"),
    }

    data class Metadata internal constructor(
        private val sanitisedCode: String,
        val format: Format,
        val separator: Char?,
        val packagingIndicator: Int?,
        val checkDigit: Char?,
        val groupLength: Int,
        val registrantLength: Int,
        val agencyName: String,
    ) {
        val publicationLength: Int
            get() = 9 - groupLength - registrantLength

        val hasCheckDigit: Boolean
            get() = checkDigit != null

        val isCheckDigitValid: Boolean
            get() {
                if (!hasCheckDigit) {
                    return false
                }
                return try {
                    if (format == Format.GTIN_14) {
                        CheckDigit.compareGTIN14(sanitisedCode, checkDigit!!)
                    } else {
                        CheckDigit.compare(sanitisedCode, checkDigit!!, format.isShort)
                    }
                } catch (e: IllegalArgumentException) {
                    false
                }
            }

        @Throws(ISBNIntegrityException::class)
        fun assertCheckDigit() {
            if (!hasCheckDigit) {
                throw ISBNIntegrityException("Supplied ISBN has no check digit", 2)
            }
            try {
                if (format == Format.GTIN_14) {
                    CheckDigit.assertGTIN14(sanitisedCode, checkDigit!!)
                } else {
                    CheckDigit.assert(sanitisedCode, checkDigit!!, format.isShort)
                }
            } catch (e: IllegalArgumentException) {
                throw ISBNIntegrityException("Failed to compare check digit - wrong input format", 3, e)
            }
        }
    }

    val gs1Element: String
        get() = gs1.toString()

    val groupElement: String
        get() = group.toString().padStart(metadata.groupLength, '0')

    val registrantElement: String
        get() = registrant.toString().padStart(metadata.registrantLength, '0')

    val publicationElement: String
        get() = publication.toString().padStart(metadata.publicationLength, '0')

    private val defaultGs1: UShort = 978u

    @Throws(ISBNConvertException::class)
    fun toISBN10(separator: Char = '-'): String {
        if (gs1 != defaultGs1) {
            throw ISBNConvertException("Input can't be converted to ISBN-10 as it contains new GS1 value $gs1 (must be 978)", 1)
        }
        val checkDigit = CheckDigit.calculate("$groupElement$registrantElement$publicationElement", true)
        return arrayOf(groupElement, registrantElement, publicationElement, checkDigit)
            .joinToString(separator.toString())
    }

    fun toISBN13(separator: Char = '-'): String {
        val checkDigit = CheckDigit.calculate("$gs1Element$groupElement$registrantElement$publicationElement", false)
        return arrayOf(gs1Element, groupElement, registrantElement, publicationElement, checkDigit)
            .joinToString(separator.toString())
    }

    /**
     * Identical to ISBN-10, but without any separators (used in barcodes)
     */
    @Throws(ISBNConvertException::class)
    fun toEAN10(): String {
        if (gs1 != defaultGs1) {
            throw ISBNConvertException("Input can't be converted to EAN-10 as it contains new GS1 value $gs1 (must be 978)", 1)
        }
        val bookNumberElement = "$groupElement$registrantElement$publicationElement"
        val checkDigit = CheckDigit.calculate(bookNumberElement, true)
        return "$bookNumberElement$checkDigit"
    }

    /**
     * Identical to ISBN-13, but without any separators (used in POS barcodes)
     */
    fun toEAN13(): String {
        val bookNumberElement = "$gs1Element$groupElement$registrantElement$publicationElement"
        val checkDigit = CheckDigit.calculate(bookNumberElement, false)
        return "$bookNumberElement$checkDigit"
    }

    /**
     * https://www.doi.org/the-identifier/resources/factsheets/doi-system-and-the-isbn-system#about-isbn-a
     */
    fun toISBNA(): String {
        val doiPrefix = "10"
        val checkDigit = CheckDigit.calculate("$gs1Element$groupElement$registrantElement$publicationElement", false)
        return "$doiPrefix.$gs1Element.$groupElement$registrantElement/$publicationElement$checkDigit"
    }

    /**
     * Contains no separators (used in logistics barcodes)
     * https://www.gtin.info/itf-14-barcodes/
     *
     * Indicator is optional if parsed from GTIN-14 (will be taken from metadata)
     *
     * @param indicator (packaging level) single-digit number from 0 to 8
     * @throws ISBNConvertException if wrong indicator is supplied or missing
     */
    @Throws(ISBNConvertException::class)
    fun toGTIN14(indicator: Int? = metadata.packagingIndicator): String {
        if (indicator === null || indicator < 0 || indicator > 8) {
            throw ISBNConvertException("Missing valid packaging indicator for GTIN14, value is ${indicator ?: "null"} (expected single digit from 0 to 8)", 2)
        }
        val bookNumberElement = "$indicator$gs1Element$groupElement$registrantElement$publicationElement"
        val checkDigit = CheckDigit.calculateForGTIN(bookNumberElement)
        return "$bookNumberElement$checkDigit"
    }

    /**
     * Convert to specified format without specifying any additional parameters.
     * This method may not work for some conversions, consider using separate methods first.
     *
     * @param keepSeparator keep original ISBN separator from the parsed code (ISBN-10/ISBN-13)
     *
     * @throws ISBNConvertException if codes are incompatible or required parameters missing in metadata
     */
    @Throws(ISBNConvertException::class)
    fun toFormat(targetFormat: Format, keepSeparator: Boolean = false): String = when(targetFormat) {
        Format.ISBN_13 -> toISBN13((if (keepSeparator) metadata.separator else null) ?: '-')
        Format.ISBN_10 -> toISBN10((if (keepSeparator) metadata.separator else null) ?: '-')
        Format.EAN_13 -> toEAN13()
        Format.EAN_10 -> toEAN10()
        Format.ISBN_A -> toISBNA()
        Format.GTIN_14 -> toGTIN14()
    }

    /**
     * Convert to the same format it was parsed from.
     * @param keepSeparator keep original ISBN separator from the parsed code (ISBN-10/ISBN-13)
     */
    fun toSourceFormat(keepSeparator: Boolean = true): String
        = toFormat(metadata.format, keepSeparator)
}