package org.http4k.traffic

import org.http4k.base64Encode
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

internal fun HttpMessage.writeTo(folder: File) {
    toFile(folder).apply {
        folder.mkdirs()
        createNewFile()
        FileOutputStream(this).use { out ->
            out.write(this@writeTo.toString().toByteArray())
            out.flush()
        }
    }
}

internal fun String.toBaseFolder(): File = File(if (isEmpty()) "." else this)

internal fun Request.toFolder(baseDir: File) = File(File(baseDir, uri.path),
    MessageDigest.getInstance("SHA1").digest(toString().toByteArray()).base64Encode()
        .replace(File.separatorChar, '_'))

internal fun HttpMessage.toFile(folder: File): File = File(folder, if (this is Request) "request.txt" else "response.txt")
