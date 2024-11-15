package org.http4k.connect.kafka.schemaregistry.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryAction
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi
import org.http4k.connect.kafka.schemaregistry.model.Subject
import org.http4k.core.Method.GET
import org.http4k.core.Request

@Http4kConnectAction
object GetSubjects : NonNullAutoMarshalledAction<Array<Subject>>(kClass(), SchemaRegistryMoshi),
    SchemaRegistryAction<Array<Subject>> {
    override fun toRequest() = Request(GET, "/subjects")
}
