package org.http4k.core

val ContentType.Companion.KAFKA_JSON_V2 get() = ContentType("application/vnd.kafka.json.v2+json")
val ContentType.Companion.KAFKA_AVRO_v2 get() = ContentType("application/vnd.kafka.avro.v2+json")
val ContentType.Companion.KAFKA_BINARY_v2 get() = ContentType("application/vnd.kafka.binary.v2+json")
val ContentType.Companion.KAFKA_JSON_SCHEMA_V2 get() = ContentType("application/vnd.kafka.jsonschema.v2+json")
val ContentType.Companion.KAFKA_PROTOBUF_V2 get() = ContentType("application/vnd.kafka.protobuf.v2+json")
