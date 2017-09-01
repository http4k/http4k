package org.http4k.multipart.part

import java.io.Closeable
import java.io.InputStream

abstract class Part(fieldName: String?, formField: Boolean, contentType: String?, fileName: String?, headers: Map<String, String>, val length: Int) : PartMetaData(fieldName, formField, contentType, fileName, headers), Closeable {

    abstract val newInputStream: InputStream

    abstract val isInMemory: Boolean

    abstract val bytes: ByteArray

    abstract val string: String
}
