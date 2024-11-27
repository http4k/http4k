package org.http4k.connect.kafka.rest.v2.model

import org.apache.avro.Schema
import org.apache.avro.generic.GenericContainer
import org.http4k.connect.model.Base64Blob
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.KAFKA_AVRO_v2
import org.http4k.core.KAFKA_BINARY_v2
import org.http4k.core.KAFKA_JSON_V2

@ExposedCopyVisibility
data class Records internal constructor(
    val records: List<Record<out Any, Any>>,
    val contentType: ContentType = APPLICATION_JSON,
    val key_schema: Schema? = null,
    val value_schema: Schema? = null
) {
    companion object {
        fun Json(records: List<Record<Any, Any>>) = Records(records, ContentType.KAFKA_JSON_V2)
        fun Avro(records: List<Record<Any, GenericContainer>>) = Records(
            records,
            ContentType.KAFKA_AVRO_v2,
            (records.first().key as? GenericContainer)?.schema,
            records.first().value.schema
        )

        fun Binary(records: List<Record<Base64Blob, Base64Blob>>) = Records(records, ContentType.KAFKA_BINARY_v2)
    }
}
