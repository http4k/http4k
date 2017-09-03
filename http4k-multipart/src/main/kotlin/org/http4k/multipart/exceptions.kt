package org.http4k.multipart

import java.io.IOException

class ParseError : RuntimeException {
    constructor(e: Exception) : super(e)

    constructor(message: String) : super(message)
}

class AlreadyClosedException : IOException()

class StreamTooLongException(message: String) : IOException(message)

class TokenNotFoundException(message: String) : IOException(message)