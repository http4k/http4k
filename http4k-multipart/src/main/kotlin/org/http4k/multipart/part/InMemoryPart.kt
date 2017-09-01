package org.http4k.multipart.part

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset

class InMemoryPart(original: PartMetaData,
                   override val bytes: ByteArray /* not immutable*/,
                   private val encoding: Charset) : Part(original.fieldName, original.isFormField, original.contentType, original.fileName, original.headers, bytes.size) {
    private var content: String? = null

    override // not a threading problem because the following calculation will always return the same value
        // and if it happens to be calculated a couple of times and assigned to content a couple of times
        // that isn't the end of the world.
    val string: String
        get() {
            if (content == null) {
                content = String(bytes, encoding)
            }
            return content!!
        }

    override val newInputStream: InputStream
        get() = ByteArrayInputStream(bytes)

    override val isInMemory = true

    override fun close() {
        // do nothing
    }
}
