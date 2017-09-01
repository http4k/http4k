package org.http4k.multipart.exceptions

import java.io.IOException

class StreamTooLongException(message: String) : IOException(message)
