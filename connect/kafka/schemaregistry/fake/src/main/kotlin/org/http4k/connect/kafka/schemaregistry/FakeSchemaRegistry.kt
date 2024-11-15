package org.http4k.connect.kafka.schemaregistry

import org.apache.avro.Schema
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.kafka.schemaregistry.SchemaRegistrationMode.manual
import org.http4k.connect.kafka.schemaregistry.endpoints.checkSchemaRegistered
import org.http4k.connect.kafka.schemaregistry.endpoints.getSchemaById
import org.http4k.connect.kafka.schemaregistry.endpoints.getSubjectVersion
import org.http4k.connect.kafka.schemaregistry.endpoints.getSubjectVersions
import org.http4k.connect.kafka.schemaregistry.endpoints.getSubjects
import org.http4k.connect.kafka.schemaregistry.endpoints.registerSchema
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters.BasicAuth
import org.http4k.routing.bind
import org.http4k.routing.routes

class FakeSchemaRegistry(
    mode: SchemaRegistrationMode = manual,
    val schemas: Storage<Schema> = Storage.InMemory()
) : ChaoticHttpHandler() {
    override val app = routes(
        BasicAuth("") { true }
            .then(
                routes(
                    checkSchemaRegistered(schemas, mode),
                    registerSchema(schemas),
                    getSubjects(schemas),
                    getSubjectVersion(schemas),
                    getSubjectVersions(schemas),
                    getSchemaById(schemas)
                )
            ),
        "" bind GET to { _ -> Response(OK).body("{}") }
    )


    /**
     * Convenience function to get a FakeSchemaRegistry client
     */
    fun client() = SchemaRegistry.Http(Credentials("", ""), Uri.of(""), this)
}

fun main() {
    FakeSchemaRegistry().start()
}
