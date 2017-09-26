package org.http4k.traffic

import org.http4k.core.HttpMessage
import org.http4k.core.Request
import java.io.File
import java.util.Base64.getEncoder

internal fun HttpMessage.writeTo(folder: File) {
    toFile(folder).apply {
        folder.mkdirs()
        createNewFile()
        writeBytes(this@writeTo.toString().toByteArray())
    }
}

internal fun String.toBaseFolder(): File = File(if (isEmpty()) "." else this)

internal fun Request.toFolder(baseDir: File) = File(File(baseDir, uri.path),
    String(getEncoder().encode(toString().toByteArray()))
        .replace(File.separatorChar, '_'))

internal fun HttpMessage.toFile(folder: File): File = File(folder, if (this is Request) "request.txt" else "response.txt")
