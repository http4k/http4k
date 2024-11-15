package org.http4k.connect.kafka.schemaregistry.endpoints

import org.apache.avro.Schema
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi.auto
import org.http4k.connect.kafka.schemaregistry.action.SchemaById
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.SCHEMA_REGISTRY
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.routing.bind

fun getSchemaById(schemas: Storage<Schema>) = "/schemas/ids/{id}" bind GET to
    { req: Request ->
        val version = Path.of("id")(req)

        schemas.keySet().firstOrNull { it.endsWith(":$version") }
            ?.let { schemas[it] }
            ?.let {
                Response(OK)
                    .with(
                        Body.auto<SchemaById>(contentType = ContentType.SCHEMA_REGISTRY).toLens() of
                            SchemaById(it)
                    )
            }
            ?: Response(NOT_FOUND)
    }
