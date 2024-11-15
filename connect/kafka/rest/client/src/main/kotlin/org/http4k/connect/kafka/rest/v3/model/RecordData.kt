package org.http4k.connect.kafka.rest.v3.model

import org.http4k.connect.kafka.rest.v3.model.RecordFormat.BINARY
import org.http4k.connect.kafka.rest.v3.model.RecordFormat.JSON
import org.http4k.connect.model.Base64Blob

sealed interface RecordData<T : Any> {
    val type: RecordFormat
    val `data`: T

    data class Binary(override val `data`: Base64Blob) : RecordData<Base64Blob> {
        override val type = BINARY
    }

    /**
     * Note that the data type here must be something which can be marshalled by the JSON marshaller,
     * so a map or a list or a primitive.
     */
    data class Json(override val `data`: Any) : RecordData<Any> {
        override val type = JSON
    }
}
