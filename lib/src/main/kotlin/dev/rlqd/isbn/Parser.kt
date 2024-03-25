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

import dev.rlqd.isbn.ranges.MusiclandRanges
import dev.rlqd.isbn.ranges.Provider
import org.jetbrains.annotations.ApiStatus

/**
 * This class parses a code string into a BookNumber instance.
 *
 * It detects the type automatically from the code length after removing non-digit characters (separators).
 * (except ISBN-A and ISMN, which are detected by the prefix)
 *
 * Once the type is detected, code integrity will be validated using check digit (unless disabled)
 * before attempting to parse it further.
 */
@ApiStatus.AvailableSince("1.0.0")
class Parser(private val rangesProvider: Provider) {
    private enum class ErrorType(val number: Int, val message: String) {
                    EMPTY_CODE(1, "No code provided"),
         UNEXPECTED_CHARACTERS(2, "Unexpected characters in the code"),
                  WRONG_LENGTH(3, "Code length is not matching any known type"),
           UNKNOWN_GS1_ELEMENT(4, "GS1 element is unknown"),
         UNKNOWN_GROUP_ELEMENT(5, "Group element is unknown"),
             NO_MATCHING_RANGE(6, "Failed to find any matching ISBN range"),
              UNRESERVED_RANGE(7, "Found matching ISBN range with 0 length"),
    }

    private class ParseException(parseError: ErrorType, currentValueDescription: String)
        : ISBNParseException("${parseError.message} at $currentValueDescription", parseError.number)

    /**
     * @param checkIntegrity throw exception if check digit is missing or incorrect (useful for validating scanned/user inputs)
     */
    @Throws(ISBNParseException::class, ISBNIntegrityException::class)
    fun parse(input: String, checkIntegrity: Boolean = true): BookNumber {
        if (input.isBlank()) {
            throw ParseException(ErrorType.EMPTY_CODE, ">$input<")
        }

        // Each method in the pipeline removes characters from the input and creates updated copy of BookNumber
        return Pair(input, BookNumber())
            .let(::extractCheckDigitAndType)
            .let { if (checkIntegrity) it.second.metadata.assertCheckDigit(); it }
            .let(::extractGs1)
            .let(::extractRegGroup)
            .let(::extractRegistrant)
            .let(::extractPublication)
            .second
    }

    private fun detectISBNSeparator(input: String): Char
        = if (with(input.trim()) { contains(' ') && !contains('-') }) ' ' else '-'

    private val separatorsMap = with(arrayOf('-', ' ', '_', '/', '.')) {
        BooleanArray(UByte.MAX_VALUE.toInt()) { contains(it.toChar()) }
    }
    private fun removeSeparators(input: String): String
        = input.filterNot { it.code < separatorsMap.size && separatorsMap[it.code] }

    private fun extractCheckDigitAndType(input: Pair<String,BookNumber>): Pair<String,BookNumber> {
        val (inputCode, bn) = input

        var code = inputCode.trim()
        val hadDoiPrefix = code.startsWith("10.")
        if (hadDoiPrefix) {
            code = code.removePrefix("10.")
        }
        if (code.startsWith("M-")) {
            code = code.replaceRange(0, 2, MusiclandRanges.PREFIX)
        }
        val possibleSeparator = detectISBNSeparator(code)
        val hadSeparators: Boolean
        code = removeSeparators(code).also {
            hadSeparators = it.length < code.length
        }
        val isMusicland = code.startsWith(MusiclandRanges.EAN_PREFIX)

        if (code.length in 12..13) {
            val type = when {
                hadDoiPrefix -> BookNumber.Type.ISBN_A
                hadSeparators && isMusicland -> BookNumber.Type.ISMN
                !hadSeparators && isMusicland -> BookNumber.Type.MUSIC_EAN
                hadSeparators -> BookNumber.Type.ISBN_13
                else -> BookNumber.Type.EAN_13
            }
            val checkDigit = when(code.length) {
                13 -> code.last().also { code = code.dropLast(1) }
                else -> null
            }
            if (checkDigit != null && !checkDigit.isDigit()) {
                throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, "$code>$checkDigit< (${type.printedName})")
            }
            val separator = when(type) {
                BookNumber.Type.ISBN_13 -> possibleSeparator
                BookNumber.Type.ISMN -> '-'
                else -> null
            }
            return Pair(
                code,
                bn.copy(metadata = bn.metadata.copy(sanitisedCode = code, type = type, checkDigit = checkDigit, separator = separator))
            )
        }

        if (code.length in 9..10) {
            val type = when {
                hadSeparators -> BookNumber.Type.ISBN_10
                else -> BookNumber.Type.EAN_10
            }
            val checkDigit = when(code.length) {
                10 -> code.last().uppercaseChar().also { code = code.dropLast(1) }
                else -> null
            }
            if (checkDigit != null && !checkDigit.isDigit() && checkDigit != 'X') {
                throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, "$code>$checkDigit< (${type.printedName})")
            }
            val separator = when(type) {
                BookNumber.Type.ISBN_10 -> possibleSeparator
                else -> null
            }
            return Pair(
                code,
                bn.copy(metadata = bn.metadata.copy(sanitisedCode = code, type = type, checkDigit = checkDigit, separator = separator))
            )
        }

        if (code.length == 14) {
            val type = BookNumber.Type.GTIN_14
            if (hadSeparators) {
                throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, ">$inputCode< (${type.printedName})")
            }

            val packagingIndicator = code.first().digitToIntOrNull()
                .let { if (it != null && it <= 8) it else null }
                ?: throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, ">${code.first()}<${code.drop(1)} (${type.printedName})")
            val checkDigit = code.last()

            return Pair(
                code.drop(1).dropLast(1),
                bn.copy(metadata = bn.metadata.copy(sanitisedCode = code, type = type, checkDigit = checkDigit, packagingIndicator = packagingIndicator))
            )
        }

        throw ParseException(ErrorType.WRONG_LENGTH, ">$code<")
    }

    private val defaultGs1: UShort = 978u
    private fun extractGs1(input: Pair<String,BookNumber>): Pair<String,BookNumber> {
        val (code, bn) = input

        if (code.length == 9) {
            return Pair(code, bn.copy(gs1 = defaultGs1))
        }

        val gs1 = code.take(3).toUShortOrNull()
            ?: throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, ">${code.take(3)}<${code.drop(3)}${bn.metadata.checkDigit ?: '_'}")

        return Pair(code.drop(3), bn.copy(gs1 = gs1))
    }

    private fun extractRegGroup(input: Pair<String,BookNumber>): Pair<String,BookNumber> {
        val (code, bn) = input

        val portionLength = when(bn.metadata.type.subset) {
            BookNumber.Subset.MUSIC -> 1
            else -> {
                val index = code.take(7).toIntOrNull()
                    ?: throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, "...>${code.take(7)}<${code.drop(7)}${bn.metadata.checkDigit ?: '_'}")
                val ranges = rangesProvider.getGs1Ranges(bn.gs1)
                    ?: throw ParseException(ErrorType.UNKNOWN_GS1_ELEMENT, ">${bn.gs1}<$code${bn.metadata.checkDigit ?: '_'}")
                val range = ranges.findRange(index)
                    ?: throw ParseException(ErrorType.NO_MATCHING_RANGE, "...>${code.take(7)}<${code.drop(7)}${bn.metadata.checkDigit ?: '_'}")
                if (range.length == 0) {
                    throw ParseException(ErrorType.UNRESERVED_RANGE, "...>${code.take(7)}<${code.drop(7)}${bn.metadata.checkDigit ?: '_'}")
                }
                range.length
            }
        }

        val group = code.take(portionLength).toUIntOrNull()
            ?: throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, "...>${code.take(portionLength)}<${code.drop(portionLength)}${bn.metadata.checkDigit ?: '_'}")

        return Pair(
            code.drop(portionLength),
            bn.copy(group = group, metadata = bn.metadata.copy(groupLength = portionLength))
        )
    }

    private fun extractRegistrant(input: Pair<String,BookNumber>): Pair<String,BookNumber> {
        val (code, bn) = input

        val ranges = when(bn.metadata.type.subset) {
            BookNumber.Subset.MUSIC -> MusiclandRanges.group
            else -> rangesProvider.getGroupRanges(bn.gs1, bn.group)
                ?: throw ParseException(ErrorType.UNKNOWN_GROUP_ELEMENT, ">${bn.gs1}-${bn.group}<$code${bn.metadata.checkDigit ?: '_'}")
        }

        val index = code.take(7).padEnd(7, '0').toIntOrNull()
            ?: throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, "...>${code.take(7)}<${code.drop(7)}${bn.metadata.checkDigit ?: '_'}")
        val range = ranges.findRange(index)
            ?: throw ParseException(ErrorType.NO_MATCHING_RANGE, "...>${code.take(7)}<${code.drop(7)}${bn.metadata.checkDigit ?: '_'}")
        if (range.length == 0) {
            throw ParseException(ErrorType.UNRESERVED_RANGE, "...>${code.take(7)}<${code.drop(7)}${bn.metadata.checkDigit ?: '_'}")
        }

        val registrant = code.take(range.length).toUIntOrNull()
            ?: throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, "...>${code.take(range.length)}<${code.drop(range.length)}${bn.metadata.checkDigit ?: '_'}")

        return Pair(
            code.drop(range.length),
            bn.copy(registrant = registrant, metadata = bn.metadata.copy(agencyName = ranges.name, registrantLength = range.length))
        )
    }

    private fun extractPublication(input: Pair<String,BookNumber>): Pair<String,BookNumber> {
        val (code, bn) = input

        // Take all that's left
        val publication = code.toUIntOrNull()
            ?: throw ParseException(ErrorType.UNEXPECTED_CHARACTERS, "...>$code<${bn.metadata.checkDigit ?: '_'}")

        return Pair(
            "",
            bn.copy(publication = publication)
        )
    }
}