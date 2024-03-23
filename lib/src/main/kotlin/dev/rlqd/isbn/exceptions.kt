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

/*
 * This file contains common exception classes, which may be thrown from main package of the library only.
 * Each exception here will have a string error code in the following format "x-y",
 * where "x" is the exception class number and "y" is the error number.
 *
 * Look through carefully for any other exceptions, if you're using other library packages directly.
 */

package dev.rlqd.isbn

import org.jetbrains.annotations.ApiStatus

@ApiStatus.AvailableSince("1.0.0")
abstract class ISBNException(message: String, cause: Throwable?, val errorCode: String): Exception(message, cause)

@ApiStatus.AvailableSince("1.0.0")
open class ISBNParseException(message: String, errorNumber: Int, cause: Throwable? = null)
    : ISBNException(message, cause, "1-$errorNumber")

@ApiStatus.AvailableSince("1.0.0")
open class ISBNIntegrityException(message: String, errorNumber: Int, cause: Throwable? = null)
    : ISBNException(message, cause, "2-$errorNumber")

@ApiStatus.AvailableSince("1.0.0")
open class ISBNValidateException(message: String, errorNumber: Int, cause: Throwable? = null)
    : ISBNException(message, cause, "3-$errorNumber")

@ApiStatus.AvailableSince("1.0.0")
open class ISBNConvertException(message: String, errorNumber: Int, cause: Throwable? = null)
    : ISBNException(message, cause, "4-$errorNumber")
