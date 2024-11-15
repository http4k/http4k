package org.http4k.connect.kafka.schemaregistry.endpoints

import org.apache.avro.Schema
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi.auto
import org.http4k.connect.kafka.schemaregistry.action.RegisteredSchemaVersion
import org.http4k.connect.kafka.schemaregistry.model.SchemaId
import org.http4k.connect.kafka.schemaregistry.model.Subject
import org.http4k.connect.kafka.schemaregistry.model.Version
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.SCHEMA_REGISTRY
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.routing.bind

fun getSubjectVersion(schemas: Storage<Schema>) = "/subjects/{subject}/versions/{version}" bind GET to
    { req: Request ->
        val subject = Path.of("subject")(req)
        val version = Path.of("version")(req)

        schemas["$subject:$version"]
            ?.let {
                Response(Status.OK)
                    .with(
                        Body.auto<RegisteredSchemaVersion>(contentType = ContentType.SCHEMA_REGISTRY).toLens() of
                            RegisteredSchemaVersion(
                                Subject.of(subject),
                                SchemaId.of(version.toInt()),
                                Version.of(version.toInt()),
                                it
                            )
                    )
            }
            ?: Response(Status.NOT_FOUND)
    }
