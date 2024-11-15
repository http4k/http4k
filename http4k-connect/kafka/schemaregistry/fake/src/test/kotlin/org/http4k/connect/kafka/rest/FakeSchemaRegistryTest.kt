package org.http4k.connect.kafka.rest

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import http4k.RandomKey
import org.http4k.connect.kafka.schemaregistry.FakeSchemaRegistry
import org.http4k.connect.kafka.schemaregistry.SchemaRegistrationMode.auto
import org.http4k.connect.kafka.schemaregistry.checkSchemaRegistered
import org.http4k.connect.successValue
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class FakeSchemaRegistryTest : SchemaRegistryContract() {
    override val http = FakeSchemaRegistry()
    override val uri = Uri.of("http://schemaregistry")

    @Test
    fun `schema lifecycle for auto registration`() {
        with(FakeSchemaRegistry(auto).client()) {
            assertThat(
                checkSchemaRegistered(subject, RandomKey.`SCHEMA$`).successValue()?.schema,
                equalTo(RandomKey.`SCHEMA$`)
            )
        }
    }
}
