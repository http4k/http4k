package org.http4k.connect.kafka.schemaregistry.action

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.apache.avro.Schema
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NullableAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryAction
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi.auto
import org.http4k.connect.kafka.schemaregistry.model.SchemaId
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
data class CheckSchemaRegistered(val subject: Subject, val schema: Schema) :
    NullableAutoMarshalledAction<RegisteredSchemaVersion>(kClass(), SchemaRegistryMoshi),
    SchemaRegistryAction<RegisteredSchemaVersion?> {
    override fun toRequest() = Request(POST, "/subjects/$subject")
        .with(Body.auto<PostedSchema>(contentType = ContentType.SCHEMA_REGISTRY).toLens() of PostedSchema(schema))
}

@JsonSerializable
data class RegisteredSchemaVersion(val subject: Subject, val id: SchemaId, val version: Version, val schema: Schema)

@JsonSerialize
data class PostedSchema(val schema: Schema)

