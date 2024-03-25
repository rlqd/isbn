# Rlqd's ISBN

A Kotlin library to parse, validate and convert International Standard Book Numbers.

<img src="barcode-book.jpg" alt="ai generated art" width="256"/>

### Highlights

* Lightweight (no third-party dependencies)
* Extensible and feature-rich
* Well-documented in KDoc
* Full test coverage
* Stable API (methods and classes annotated by `@ApiStatus`)

### Supported types

* ISBN-13
* ISBN-10
* ISMN (printed music)
* ISBN-A
* GTIN-14
* EAN-13 representation of ISBN-13 or ISMN
* EAN-10 representation of ISBN-10

ISBN types supported with hyphen '-' or space ' ' separator.
ISBN-10 'X' check digit is also supported.

ISBN and EAN types can be parsed with or without check digit,
which allows to use the library as a check digit calculator.

### How to install

The library is available through Maven Central

Example for Gradle:

```kotlin
// build.gradle.kts

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.rlqd.libs:isbn:1.1.0")
}
```

#### Versions

Library version has the following format consisting of 3 numbers "x.y.z"

Meaning of version number change:

* x - major, potentially breaking changes
* y - new features, potentially breaking changes to **experimental** classes only
* z - minor fixes and embedded ranges updates

How it's reflected in the code:

Each class and some of the methods are annotated with one of the following:

* `@ApiStatus.Experimental` - subject to change in further releases
* `@ApiStatus.AvailableSince("x.y.z")` - author is committed to maintain backward compatibility, except major releases

### Kudos

API influenced by similar PHP library: https://github.com/biblys/isbn

### Readme contents

&nbsp;&nbsp;[I. Core principals](#i-core-principals)  
&nbsp;[II. Entry point](#ii-entry-point)  
[III. Book Number](#iii-booknumber)  
[IV. Ranges Provider](#iv-ranges-providers)  
&nbsp;[V. Exceptions](#v-exceptions)


## I. Core principals

Parsing ISBNs requires ranges information published on https://www.isbn-international.org/range_file_generation

In the library, a flexible approach has been implemented through [Provider](#iv-ranges-providers) interface.

Default implementation uses embedded ranges, obtained during the library build process.
Alternative (experimental) option allows fetching them online through unofficial api.

## II. Entry point

`dev.rlqd.isbn.ISBN` is a main class of the library,
which contains methods and aliases for most common use cases.

It can be used directly as an object (companion object `ISBN.Default`)
or a separate instance with custom provider could be created using `ISBN.Custom()`.

### Usage examples

```kotlin
import dev.rlqd.isbn.*

// Parse the code to get information about it (type is auto-detected)
val bookNumber: BookNumber = ISBN.parse("978-5-17-095179-6")

// Convert the code to a given type (ISBN-10 in this case)
val isbn10: String = ISBN.convertToISBN10("978-5-17-095179-6")
println(isbn10) // "5-17-095179-5"

// Validate the code is of a given type and properly formatted
try {
    ISBN.validateAsISBN13("978-5-17-095179-6")
    println("The code is valid ISBN-13!")
} catch (e: ISBNException) {
    println("Something is wrong: ${e.message}")
}

// Validate the code is of any supported type and properly formatted
try {
    val bn: BookNumber = ISBN.validateAsAny("978-5-17-095179-6")
    println("The code is valid ${bn.type.printedName}!")
} catch (e: ISBNException) {
    println("Something is wrong: ${e.message}")
}
```

### Methods reference

Input string can contain any supported code type.
Please read KDoc for details and note [exceptions](#v-exceptions).

#### Parse

Method returns an instance of [BookNumber](#iii-booknumber).

Doesn't validate that input is properly formatted, but ensures integrity with a check digit.
Integrity check can be disabled to validate it later or to parse ISBNs without a check digit.

`parse(input, checkIntegrity = true)`

#### Convert

Methods return a properly formatted string

* `convertToISBN13(input, separator = '-')`
* `convertToISBN10(input, separator = '-')`
* `convertToEAN13(input)`
* `convertToEAN10(input)`
* `convertToISBNA(input)`
* `convertToGTIN14(input, indicator)`
* `convertToISMN(input)`
* `convertToMusicEAN(input)`

#### Validate

Methods return an instance of [BookNumber](#iii-booknumber)

* `validateAsISBN13(input)`
* `validateAsISBN10(input)`
* `validateAsEAN13(input)`
* `validateAsEAN10(input)`
* `validateAsISBNA(input)`
* `validateAsGTIN14(input)`
* `validateAsISMN(input)`
* `validateAsMusicEAN(input)`


* `validateAsType(input, type)` target format is a enum value
* `validateAsAny(input)` accepts any supported format

## III. BookNumber

`dev.rlqd.isbn.BookNumber` is a data class containing detailed information about the parsed code
as well as helper methods for conversion to other formats and check digit validation.

### Properties

BookNumber class properties contain elements as defined by ISBN standard.

Parsed from ISBN 978-5-17-095179-6

* `gs1 = 978u`
* `group = 5u`
* `registrant = 17u`
* `publication = 95179u`


* `gs1Element = "978"`
* `groupElement = "5"`
* `registrantElement = "17"`
* `publicationElement = "095179"`


* `metadata = BookNumber.Metadata(...)`: [Metadata](#metadata-properties)

### Methods

All methods return a properly formatted string.
Some may throw `ISBNConvertException`.

* `toISBN10(separator = '-')`
* `toISBN13(separator = '-')`
* `toEAN10()`
* `toEAN13()`
* `toISBNA()`
* `toGTIN14()`
* `toISMN()`
* `toMusicEAN()`


* `toFormat(targetFormat, keepSeparator = false)`
* `toSourceFormat(keepSeparator = true)`

### Metadata properties

Parsed from ISBN 978-5-17-095179-6

* `type = BookNumber.Type.ISBN_13` (enum)
* `separator = '-'`
* `packagingIndicator = null` (GTIN-14 specific)


* `checkDigit = '6'`
* `hasCheckDigit = true`
* `isCheckDigitValid = true`


Ranges information

* `groupLength = 1`
* `registrantLength = 2`
* `publicationLength = 6`


* `agencyName = "former U.S.S.R"`

### Metadata methods

`assertCheckDigit()` throws `ISBNIntegrityException` if check digit is missing or wrong

## IV. Ranges providers

The library can't operate without the ISBN ranges information.
Ranges are provided by the implementations of the `dev.rlqd.ranges.Provider` interface.

### Default provider

`dev.rlqd.ranges.DefaultProvider`

By default, the library uses ranges embedded into jar, which were obtained during library build process.

Location in the jar: `dev/rlqd/isbn/ranges/isbn-ranges.json`

This ensures the library always works "out of the box"
and allows to use it without internet connection and filesystem access.
The downside of this approach is that the ranges are always as fresh as a library package.

### Online provider

`dev.rlqd.ranges.OnlineProvider`

Note: Online provider will stay in experimental status and is subject to change.
May as well **stop working unexpectedly** as it uses unofficial api for fetching ranges
(emulates html form on isbn-international website).

Be mindful when using this method.
If you intend to operate multi-instance service and/or frequently update ranges,
consider [implementing your own](#implement-your-own-provider) provider.

#### Default configuration

OnlineProvider caches ranges in the filesystem tmp directory.
Cache TTL is set to 30 days as a safety measure.
You can adjust this value by constructing your own cache instance.

If you want to implement a different cache storage, see interface `dev.rlqd.ranges.cache.Cache`.

Timeouts for the ranges download are quite relaxed (10 sec connect, 60 sec read).

#### How to use

```kotlin
// Simple use case with default configuration
val isbn = ISBN.Custom(OnlineProvider())
val bookNumber = isbn.parse("978-5-17-095179-6")

// Custom cache (consider reusing the instance to avoid frequent filesystem access)
OnlineProvider(cache = FileCache(ttl = 86400000L /* 1 day */))

// Custom timeouts
OnlineProvider(client = DownloadClient(connectTimeout = 1000, readTimeout = 10000))

// Custom cache and timeouts
OnlineProvider(FileCache(86400000L), DownloadClient(1000, 10000))
```

### Implement-your-own provider

`dev.rlqd.ranges.Provider` interface

Implementing a new provider is straightforward - you need to override one method `getRanges(prefix)`
which returns `dev.rlqd.ranges.RangesGroup` by a corresponding string prefix.

You can obtain the ranges by using `dev.rlqd.ranges.utils.DownloadClient` class (experimental).
It's then necessary to store this data somewhere.

Download client fetches RangesMessage.xml from isbn-international website
and converts it to the RangesGroup map using `dev.rlqd.ranges.utils.Reader` object.

Good approach for a scalable system will be implementing a separate service
which will maintain ranges information relevance and store them somewhere (e.g. Redis).
Then the custom provider can fetch the latest ranges information from your service (or Redis)
and cache it locally with low TTL.

```kotlin
class ExampleRangesProvider: Provider {
    // Obtained from DownloadClient / Redis / etc.
    private val map = mapOf<String,RangesGroup>(
        "978" to RangeGroup(/* ... */),
        "978-1" to RangeGroup(/* ... */),
        /* ... */
    )

    override fun getRanges(prefix: String): RangeGroup? = map[prefix]
}
```

## V. Exceptions

Exception is an important aspect of using this library for parsing and validation,
as they can reveal what exactly is wrong with the code.
Look for `@Throws` annotations and KDoc, exceptions vary by method.

Note: exceptions are only organised in the main classes of the library (`dev.rlqd.isbn` package).
Look carefully for annotations if you're using any other packages directly
(e.g. `dev.rlqd.isbn.ranges`).

All exceptions share one abstract class `dev.rlqd.isbn.ISBNException`
which has `errorCode` property providing unique code for each error.

| Class                  | Error code | Description                                                 |
|------------------------|------------|-------------------------------------------------------------|
| ISBNParseException     | 1-1        | No input code provided (or empty)                           |
| ISBNParseException     | 1-2        | Unexpected characters in the code                           |
| ISBNParseException     | 1-3        | Code length is not matching any known format                |
| ISBNParseException     | 1-4        | GS1 element is unknown (missing from ranges)                |
| ISBNParseException     | 1-5        | Group element is unknown (missing from ranges)              |
| ISBNParseException     | 1-6        | Failed to find any matching ISBN range                      |
| ISBNParseException     | 1-7        | Found matching ISBN range with 0 length                     |
| ISBNIntegrityException | 2-1        | Supplied check digit doesn't match the calculated value     |
| ISBNIntegrityException | 2-2        | Code has no check digit                                     |
| ISBNIntegrityException | 2-3        | Can't compare check digit because of wrong input format     |
| ISBNValidateException  | 3-1        | Incorrectly formatted code (misplaced hyphen, etc.)         |
| ISBNValidateException  | 3-2        | Detected different format instead                           |
| ISBNConvertException   | 4-1        | Can't convert the code with new GS1 value to legacy ISBN-10 |
| ISBNConvertException   | 4-2        | Missing required information (e.g. GTIN-14 packaging level) |
| ISBNConvertException   | 4-3        | Conversion attempted between incompatible formats (ISMN)    |
