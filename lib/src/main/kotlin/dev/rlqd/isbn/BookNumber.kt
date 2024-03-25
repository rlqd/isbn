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
        Metadata("", Type.ISBN_13, null, null, null, 0, 0, ""),
    )

    /**
     * Subsets of types.
     * Only types in the same subset are compatible for conversion.
     */
    enum class Subset {
        DEFAULT,
        MUSIC,
    }

    /**
     * Code types supported by the library.
     */
    enum class Type(
        val printedName: String,
        val isShort: Boolean = false,
        val subset: Subset = Subset.DEFAULT,
    ) {
        ISBN_13("ISBN-13"),
        ISBN_10("ISBN-10", true),
        EAN_13("EAN-13"),
        EAN_10("EAN-10", true),
        ISBN_A("ISBN-A"),
        GTIN_14("GTIN-14"),
        ISMN("ISMN", subset = Subset.MUSIC),
        MUSIC_EAN("EAN-13/ISMN", subset = Subset.MUSIC)
    }

    data class Metadata internal constructor(
        private val sanitisedCode: String,
        val type: Type,
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
                    if (type == Type.GTIN_14) {
                        CheckDigit.compareGTIN14(sanitisedCode, checkDigit!!)
                    } else {
                        CheckDigit.compare(sanitisedCode, checkDigit!!, type.isShort)
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
                if (type == Type.GTIN_14) {
                    CheckDigit.assertGTIN14(sanitisedCode, checkDigit!!)
                } else {
                    CheckDigit.assert(sanitisedCode, checkDigit!!, type.isShort)
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

    /**
     * ISBN-10 format (old standard).
     *
     * @throws ISBNConvertException if incompatible types; if new gs1 is used
     */
    @Throws(ISBNConvertException::class)
    fun toISBN10(separator: Char = '-'): String {
        assertConversion(Type.ISBN_10)
        if (gs1 != defaultGs1) {
            throw ISBNConvertException("Input can't be converted to ISBN-10 as it contains new GS1 value $gs1 (must be 978)", 1)
        }
        val checkDigit = CheckDigit.calculate("$groupElement$registrantElement$publicationElement", true)
        return arrayOf(groupElement, registrantElement, publicationElement, checkDigit)
            .joinToString(separator.toString())
    }

    /**
     * ISBN-13 format (current standard).
     *
     * @throws ISBNConvertException if incompatible types
     */
    @Throws(ISBNConvertException::class)
    fun toISBN13(separator: Char = '-'): String {
        assertConversion(Type.ISBN_13)
        return toBaseISBN(separator)
    }

    /**
     * EAN-10 representation of ISBN-10 (used in barcodes).
     * Identical to ISBN-10, but without any separators.
     *
     * @throws ISBNConvertException if incompatible types; if new gs1 is used
     */
    @Throws(ISBNConvertException::class)
    fun toEAN10(): String {
        assertConversion(Type.EAN_10)
        if (gs1 != defaultGs1) {
            throw ISBNConvertException("Input can't be converted to EAN-10 as it contains new GS1 value $gs1 (must be 978)", 1)
        }
        val bookNumberElement = "$groupElement$registrantElement$publicationElement"
        val checkDigit = CheckDigit.calculate(bookNumberElement, true)
        return "$bookNumberElement$checkDigit"
    }

    /**
     * EAN-13 representation of ISBN-13 (used in barcodes).
     * Identical to ISBN-13, but without any separators.
     *
     * @throws ISBNConvertException if incompatible types
     */
    @Throws(ISBNConvertException::class)
    fun toEAN13(): String {
        assertConversion(Type.EAN_13)
        return toBaseISBN(null)
    }

    /**
     * https://www.doi.org/the-identifier/resources/factsheets/doi-system-and-the-isbn-system#about-isbn-a
     *
     * @throws ISBNConvertException if incompatible types
     */
    @Throws(ISBNConvertException::class)
    fun toISBNA(): String {
        assertConversion(Type.ISBN_A)
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
     * @throws ISBNConvertException if incompatible types; if wrong indicator is supplied or missing
     */
    @Throws(ISBNConvertException::class)
    fun toGTIN14(indicator: Int? = metadata.packagingIndicator): String {
        assertConversion(Type.GTIN_14)
        if (indicator === null || indicator < 0 || indicator > 8) {
            throw ISBNConvertException("Missing valid packaging indicator for GTIN14, value is ${indicator ?: "null"} (expected single digit from 0 to 8)", 2)
        }
        val bookNumberElement = "$indicator$gs1Element$groupElement$registrantElement$publicationElement"
        val checkDigit = CheckDigit.calculateForGTIN(bookNumberElement)
        return "$bookNumberElement$checkDigit"
    }

    /**
     * International Standard Music Number (subset of ISBN).
     * Format identical to ISBN-13 separated by hyphens.
     *
     * @throws ISBNConvertException if tried to convert from non-music type
     */
    @Throws(ISBNConvertException::class)
    fun toISMN(): String {
        assertConversion(Type.ISMN)
        return toBaseISBN('-')
    }

    /**
     * International Standard Music Number (subset of ISBN-13) represented as EAN-13
     * (same as ISMN, but without separators).
     *
     * @throws ISBNConvertException if tried to convert from non-music type
     */
    @Throws(ISBNConvertException::class)
    fun toMusicEAN(): String {
        assertConversion(Type.MUSIC_EAN)
        return toBaseISBN(null)
    }

    /**
     * Convert to specified type and format.
     * This method may not work for some conversions, consider using separate methods first.
     *
     * @param keepSeparator keep original ISBN separator from the parsed code (ISBN-10/ISBN-13)
     *
     * @throws ISBNConvertException if types are incompatible or required parameters missing in metadata
     */
    @Throws(ISBNConvertException::class)
    fun toFormat(targetType: Type, keepSeparator: Boolean = false): String = when(targetType) {
        Type.ISBN_13 -> toISBN13((if (keepSeparator) metadata.separator else null) ?: '-')
        Type.ISBN_10 -> toISBN10((if (keepSeparator) metadata.separator else null) ?: '-')
        Type.EAN_13 -> toEAN13()
        Type.EAN_10 -> toEAN10()
        Type.ISBN_A -> toISBNA()
        Type.GTIN_14 -> toGTIN14()
        Type.ISMN -> toISMN()
        Type.MUSIC_EAN -> toMusicEAN()
    }

    /**
     * Convert to the same type and format it was parsed from.
     * @param keepSeparator keep original ISBN separator from the parsed code (ISBN-10/ISBN-13)
     */
    fun toSourceFormat(keepSeparator: Boolean = true): String
        = toFormat(metadata.type, keepSeparator)

    private fun toBaseISBN(separator: Char?): String {
        val plain = "$gs1Element$groupElement$registrantElement$publicationElement"
        val checkDigit = CheckDigit.calculate(plain, false)
        if (separator == null) {
            return "$plain$checkDigit"
        }
        return arrayOf(gs1Element, groupElement, registrantElement, publicationElement, checkDigit)
            .joinToString(separator.toString())
    }

    @Throws(ISBNConvertException::class)
    private fun assertConversion(targetType: Type) {
        if (metadata.type.subset != targetType.subset) {
            throw ISBNConvertException("Can't convert ${metadata.type.printedName} to ${targetType.printedName}", 3)
        }
    }
}