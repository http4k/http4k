package org.http4k.connect.kafka.schemaregistry.endpoints

import org.apache.avro.Schema
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi.auto
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.SCHEMA_REGISTRY
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.routing.bind

fun getSubjectVersions(schemas: Storage<Schema>) = "/subjects/{subject}/versions" bind Method.GET to
    { req: Request ->
        val subject = Path.of("subject")(req)

        schemas.keySet().filter { it.startsWith(subject) }
            .map { it.substringAfter(":") }
            .takeIf { it.isNotEmpty() }
            ?.let {
                Response(OK)
                    .with(Body.auto<List<String>>(contentType = ContentType.SCHEMA_REGISTRY).toLens() of it)
            }
            ?: Response(Status.NOT_FOUND)
    }
