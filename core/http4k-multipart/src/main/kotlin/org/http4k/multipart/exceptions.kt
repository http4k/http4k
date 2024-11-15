package org.http4k.multipart

import java.io.IOException

internal class ParseError : RuntimeException {
    constructor(e: Exception) : super(e)

    constructor(message: String) : super(message)
}

internal class AlreadyClosedException : IOException()

internal class StreamTooLongException(message: String) : IOException(message)

internal class TokenNotFoundException(message: String) : IOException(message)
