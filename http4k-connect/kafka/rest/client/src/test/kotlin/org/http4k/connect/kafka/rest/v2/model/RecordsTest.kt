package org.http4k.connect.kafka.rest.v2.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import http4k.RandomKey
import org.junit.jupiter.api.Test
import java.util.UUID

class RecordsTest {

    @Test
    fun `sets key schema when key is an avro object`() {
        val avroObject = RandomKey(UUID.randomUUID())

        assertThat(
            Records.Avro(
                listOf(Record(avroObject, avroObject))
            ).key_schema, equalTo(avroObject.schema)
        )

        assertThat(
            Records.Avro(
                listOf(Record("value", avroObject))
            ).key_schema, equalTo(null)
        )
    }

}
