package org.http4k.multipart.part

import java.io.InputStream

class StreamingPart(fieldName: String, formField: Boolean, contentType: String?, fileName: String?, val inputStream: InputStream, headers: Map<String, String>)
    : PartMetaData(fieldName, formField, contentType, fileName, headers) {

    val contentsAsString: String
        get() = inputStream.use { it.reader().readText() }
}
