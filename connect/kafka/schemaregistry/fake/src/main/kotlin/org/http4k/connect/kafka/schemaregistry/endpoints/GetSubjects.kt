package org.http4k.connect.kafka.schemaregistry.endpoints

import org.apache.avro.Schema
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi.auto
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.SCHEMA_REGISTRY
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind

fun getSubjects(schemas: Storage<Schema>) = "/subjects" bind GET to
    { _: Request ->
        Response(OK)
            .with(
                Body.auto<List<String>>(contentType = ContentType.SCHEMA_REGISTRY)
                    .toLens() of schemas.keySet().map { it.substringBefore(":") }.toList()
            )
    }
