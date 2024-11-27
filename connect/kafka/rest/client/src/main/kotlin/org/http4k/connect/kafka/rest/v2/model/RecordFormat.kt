package org.http4k.connect.kafka.rest.v2.model

import org.http4k.core.ContentType
import org.http4k.core.KAFKA_AVRO_v2
import org.http4k.core.KAFKA_BINARY_v2
import org.http4k.core.KAFKA_JSON_SCHEMA_V2
import org.http4k.core.KAFKA_JSON_V2
import org.http4k.core.KAFKA_PROTOBUF_V2

enum class RecordFormat(val contentType: ContentType) {
    binary(ContentType.KAFKA_BINARY_v2),
    avro(ContentType.KAFKA_AVRO_v2),
    json(ContentType.KAFKA_JSON_V2),
    jsonschema(ContentType.KAFKA_JSON_SCHEMA_V2),
    protobuf(ContentType.KAFKA_PROTOBUF_V2)
}
