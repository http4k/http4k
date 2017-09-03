package org.http4k.multipart.exceptions

class ParseError : RuntimeException {
    constructor(e: Exception) : super(e)

    constructor(message: String) : super(message)
}
