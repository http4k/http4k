package org.http4k.multipart

import java.io.InputStream

internal class StreamingPart(fieldName: String, formField: Boolean, contentType: String?, fileName: String?, val inputStream: InputStream, headers: Map<String, String>) :
    PartMetaData(fieldName, formField, contentType, fileName, headers) {

    val contentsAsString: String
        get() = inputStream.use { it.reader().readText() }
}
