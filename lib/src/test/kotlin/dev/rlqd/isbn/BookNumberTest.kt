package dev.rlqd.isbn

import kotlin.test.Test
import kotlin.test.assertEquals

class BookNumberTest {
    @Test
    fun format() {
        val bn = BookNumber(
            gs1 = 978u,
            group = 5u,
            registrant = 17u,
            publication = 95179u,
            BookNumber.Metadata(
                format = BookNumber.Format.ISBN_13,
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
            { bn.toISBN10() } to "5-17-095179-5",
            { bn.toISBN13() } to "978-5-17-095179-6",
            bn::toEAN10 to "5170951795",
            bn::toEAN13 to "9785170951796",
            bn::toISBNA to "10.978.517/0951796",
            { bn.toGTIN14() } to "09785170951796",
        ).forEach { (func, output) ->
            assertEquals(output, func())
        }
    }
}