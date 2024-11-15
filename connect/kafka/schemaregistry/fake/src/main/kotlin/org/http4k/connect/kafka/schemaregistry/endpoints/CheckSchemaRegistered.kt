package org.http4k.connect.kafka.schemaregistry.endpoints

import org.apache.avro.Schema
import org.http4k.connect.kafka.schemaregistry.SchemaRegistrationMode
import org.http4k.connect.kafka.schemaregistry.SchemaRegistrationMode.auto
import org.http4k.connect.kafka.schemaregistry.SchemaRegistrationMode.manual
import org.http4k.connect.kafka.schemaregistry.SchemaRegistryMoshi.auto
import org.http4k.connect.kafka.schemaregistry.action.PostedSchema
import org.http4k.connect.kafka.schemaregistry.action.RegisteredSchemaVersion
import org.http4k.connect.kafka.schemaregistry.model.SchemaId
import org.http4k.connect.kafka.schemaregistry.model.Subject
import org.http4k.connect.kafka.schemaregistry.model.Version
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.SCHEMA_REGISTRY
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.routing.bind
import kotlin.math.absoluteValue

fun checkSchemaRegistered(schemas: Storage<Schema>, mode: SchemaRegistrationMode) = "/subjects/{subject}" bind POST to
    { req: Request ->
        val subject = Path.of("subject")(req)

        val posted = Body.auto<PostedSchema>().toLens()(req).schema

        when (schemas.keySet().firstOrNull { schemas[it] == posted }) {
            null -> when (mode) {
                auto -> {
                    schemas["$subject:${posted.toString().hashCode()}"] = posted
                    ok(subject, posted)
                }

                manual -> Response(NOT_FOUND)
            }

            else -> ok(subject, posted)
        }
    }

private fun ok(subject: String, schema: Schema) =
    Response(OK).with(
        Body.auto<RegisteredSchemaVersion>(contentType = ContentType.SCHEMA_REGISTRY)
            .toLens() of RegisteredSchemaVersion(
            Subject.of(subject),
            SchemaId.of(schema.toString().hashCode().absoluteValue),
            Version.of(schema.toString().hashCode().absoluteValue),
            schema
        )
    )
