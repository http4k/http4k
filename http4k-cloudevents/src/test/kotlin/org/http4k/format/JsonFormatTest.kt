package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.cloudevents.CloudEventData
import io.cloudevents.core.builder.CloudEventBuilder.v1
import io.cloudevents.core.builder.withSource
import io.cloudevents.jackson.JsonCloudEventData
import io.cloudevents.with
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

data class MyCloudEventData(val value: Int) : CloudEventData {
    override fun toBytes() = value.toString().toByteArray()

    companion object {
        fun fromStringBytes(bytes: ByteArray) = MyCloudEventData(Integer.valueOf(String(bytes)))
    }
}

class JsonFormatTest {
    @Test
    fun `can roundtrip data into an event`() {
        val data = MyCloudEventData(123)
        val empty = v1()
            .withId("123")
            .withType("type")
            .withSource(Uri.of("http4k"))
            .build()

        val cloudEventDataLens = Jackson.cloudEventDataLens<MyCloudEventData>()

        val withData = empty.with(cloudEventDataLens of data)
        assertThat(
            Jackson.compact((withData.data as JsonCloudEventData).node),
            equalTo("""{"value":123}""")
        )

        assertThat(cloudEventDataLens(withData), equalTo(data))
    }
}
