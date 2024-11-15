package org.http4k.connect.kafka.schemaregistry.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NullableAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryAction
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi
import org.http4k.connect.kafka.schemaregistry.model.Subject
import org.http4k.connect.kafka.schemaregistry.model.Version
import org.http4k.core.Method
import org.http4k.core.Request

@Http4kConnectAction
data class GetSubjectVersion(val subject: Subject, val version: Version) :
    NullableAutoMarshalledAction<RegisteredSchemaVersion>(kClass(), SchemaRegistryMoshi),
    SchemaRegistryAction<RegisteredSchemaVersion?> {
    override fun toRequest() = Request(Method.GET, "/subjects/$subject/versions/$version")
}
