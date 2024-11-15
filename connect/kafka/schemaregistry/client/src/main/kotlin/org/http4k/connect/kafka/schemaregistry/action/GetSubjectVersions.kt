package org.http4k.connect.kafka.schemaregistry.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryAction
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi
import org.http4k.connect.kafka.schemaregistry.model.Subject
import org.http4k.connect.kafka.schemaregistry.model.Version
import org.http4k.core.Method
import org.http4k.core.Request

@Http4kConnectAction
data class GetSubjectVersions(val subject: Subject) :
    NonNullAutoMarshalledAction<Array<Version>>(kClass(), SchemaRegistryMoshi), SchemaRegistryAction<Array<Version>> {
    override fun toRequest() = Request(Method.GET, "/subjects/$subject/versions")
}
