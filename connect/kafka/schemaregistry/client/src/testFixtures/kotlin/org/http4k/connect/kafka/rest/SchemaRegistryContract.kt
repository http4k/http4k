package org.http4k.connect.kafka.rest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.hasElement
import http4k.RandomKey.`SCHEMA$`
import org.http4k.connect.kafka.schemaregistry.Http
import org.http4k.connect.kafka.schemaregistry.SchemaRegistry
import org.http4k.connect.kafka.schemaregistry.action.RegisteredSchemaVersion
import org.http4k.connect.kafka.schemaregistry.action.SchemaById
import org.http4k.connect.kafka.schemaregistry.checkSchemaRegistered
import org.http4k.connect.kafka.schemaregistry.getSchemaById
import org.http4k.connect.kafka.schemaregistry.getSubjectVersion
import org.http4k.connect.kafka.schemaregistry.getSubjectVersions
import org.http4k.connect.kafka.schemaregistry.getSubjects
import org.http4k.connect.kafka.schemaregistry.model.SchemaId
import org.http4k.connect.kafka.schemaregistry.model.SchemaType.AVRO
import org.http4k.connect.kafka.schemaregistry.model.Subject
import org.http4k.connect.kafka.schemaregistry.registerSchema
import org.http4k.connect.successValue
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

abstract class SchemaRegistryContract {

    abstract val http: HttpHandler
    abstract val uri: Uri

    val subject = Subject.of(UUID.randomUUID().toString())

    private val schemaRegistry by lazy {
        SchemaRegistry.Http(Credentials("", ""), uri, http)
    }

    @BeforeEach
    fun `can get to proxy`() {
        assumeTrue(http(Request(GET, uri)).status == OK)
    }

    @Test
    fun `schema lifecycle`() {
        with(schemaRegistry) {
            assertThat(checkSchemaRegistered(subject, `SCHEMA$`).successValue(), equalTo(null))

            registerSchema(
                subject,
                `SCHEMA$`,
                AVRO,
                listOf()
            ).successValue()

            val registeredSchema = checkSchemaRegistered(subject, `SCHEMA$`).successValue()!!

            assertThat(
                registeredSchema.schema,
                equalTo(`SCHEMA$`)
            )

            assertThat(
                getSubjects().successValue().toList(),
                hasElement(subject)
            )

            val version = getSubjectVersions(subject).successValue().toList().first()

            assertThat(
                version.value,
                greaterThan(0)
            )

            assertThat(
                getSubjectVersion(subject, version).successValue(),
                equalTo(RegisteredSchemaVersion(subject, SchemaId.of(version.value), version, `SCHEMA$`))
            )

            assertThat(
                getSchemaById(registeredSchema.id).successValue(),
                equalTo(SchemaById(`SCHEMA$`))
            )
        }
    }

}
