package org.http4k.multipart.part

import org.http4k.multipart.stream.StreamUtil
import java.io.InputStream
import java.nio.charset.StandardCharsets

class StreamingPart(fieldName: String?, formField: Boolean, contentType: String?, fileName: String?, val inputStream: InputStream, headers: Map<String, String>) : PartMetaData(fieldName, formField, contentType, fileName, headers) {

    val contentsAsString: String
        get() = StreamUtil.readStringFromInputStream(inputStream, StandardCharsets.UTF_8, 4096)


}
