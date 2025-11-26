package org.http4k.connect.kafka.schemaregistry.action

import org.apache.avro.Schema
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NullableAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryAction
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi
import org.http4k.connect.kafka.schemaregistry.model.SchemaId
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class GetSchemaById(val id: SchemaId) : NullableAutoMarshalledAction<SchemaById>(kClass(), SchemaRegistryMoshi),
    SchemaRegistryAction<SchemaById?> {
    override fun toRequest() = Request(GET, "/schemas/ids/$id")
}

@JsonSerializable
data class SchemaById(val schema: Schema)
