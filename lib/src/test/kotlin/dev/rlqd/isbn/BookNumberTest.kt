package dev.rlqd.isbn

import kotlin.test.Test
import kotlin.test.assertEquals

class BookNumberTest {
    @Test
    fun formatDefault() {
        val bn = BookNumber(
            gs1 = 978u,
            group = 5u,
            registrant = 17u,
            publication = 95179u,
            BookNumber.Metadata(
                type = BookNumber.Type.ISBN_13,
                separator = '-',
                sanitisedCode = "978517095179",
                packagingIndicator = 0,
                checkDigit = '6',
                groupLength = 1,
                registrantLength = 2,
                agencyName = "former U.S.S.R",
            ),
        )
        mapOf(
            BookNumber.Type.ISBN_10 to "5-17-095179-5",
            BookNumber.Type.ISBN_13 to "978-5-17-095179-6",
            BookNumber.Type.EAN_10 to "5170951795",
            BookNumber.Type.EAN_13 to "9785170951796",
            BookNumber.Type.ISBN_A to "10.978.517/0951796",
            BookNumber.Type.GTIN_14 to "09785170951796",
        ).forEach { (type, output) ->
            assertEquals(output, bn.toFormat(type))
        }
    }

    @Test
    fun formatMusic() {
        val bn = BookNumber(
            gs1 = 979u,
            group = 0u,
            registrant = 2600u,
            publication = 43u,
            BookNumber.Metadata(
                type = BookNumber.Type.ISMN,
                separator = '-',
                sanitisedCode = "979026000043",
                packagingIndicator = 0,
                checkDigit = '8',
                groupLength = 1,
                registrantLength = 4,
                agencyName = "Musicland",
            ),
        )
        mapOf(
            BookNumber.Type.ISMN to "979-0-2600-0043-8",
            BookNumber.Type.MUSIC_EAN to "9790260000438",
        ).forEach { (type, output) ->
            assertEquals(output, bn.toFormat(type))
        }
    }
}