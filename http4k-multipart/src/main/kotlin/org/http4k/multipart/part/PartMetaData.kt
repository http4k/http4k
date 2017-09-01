package org.http4k.multipart.part

import java.util.*

abstract class PartMetaData(val fieldName: String?, val isFormField: Boolean, val contentType: String?, val fileName: String?, headers: Map<String, String>) {
    val headers: Map<String, String> = Collections.unmodifiableMap(headers)

    fun sink() {
        throw UnsupportedOperationException("sink not implemented")
    }

}
