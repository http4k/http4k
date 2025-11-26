package org.http4k.connect.kafka.schemaregistry.action

import org.apache.avro.Schema
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryAction
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi.auto
import org.http4k.connect.kafka.schemaregistry.model.SchemaId
import org.http4k.connect.kafka.schemaregistry.model.SchemaName
import org.http4k.connect.kafka.schemaregistry.model.SchemaType
import org.http4k.connect.kafka.schemaregistry.model.Subject
import org.http4k.connect.kafka.schemaregistry.model.Version
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.SCHEMA_REGISTRY
import org.http4k.core.with
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
data class RegisterSchema(
    val subject: Subject,
    val schema: Schema,
    val schemaType: SchemaType,
    val references: List<References>
) : NonNullAutoMarshalledAction<RegisteredSchema>(kClass(), SchemaRegistryMoshi),
    SchemaRegistryAction<RegisteredSchema> {
    override fun toRequest() = Request(POST, "/subjects/$subject/versions")
        .with(
            Body.auto<RegisterSchemaVersionReq>(contentType = ContentType.SCHEMA_REGISTRY)
                .toLens() of RegisterSchemaVersionReq(schema, schemaType, references)
        )
}

@JsonSerializable
data class References(
    val name: SchemaName,
    val subject: Subject,
    val version: Version
)

@JsonSerializable
data class RegisteredSchema(val id: SchemaId)

@JsonSerializable
data class RegisterSchemaVersionReq(
    val schema: Schema,
    val schemaType: SchemaType,
    val references: List<References>
)

