package org.http4k.filter

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import java.io.File

internal fun HttpMessage.writeTo(folder: File) {
    toFile(folder).apply {
        folder.mkdirs()
        createNewFile()
        writeBytes(toString().toByteArray())
    }
}

internal fun String.toBaseFolder(): File = File(if (isEmpty()) "." else this)

internal fun HttpMessage.toFile(folder: File): File = File(folder, if (this is Request) "request.txt" else "response.txt")
