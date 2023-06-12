package io.cloudevents.http4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.cloudevents.CloudEventData
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.builder.withDataContentType
import io.cloudevents.core.builder.withDataSchema
import io.cloudevents.core.builder.withSource
import io.cloudevents.core.data.BytesCloudEventData
import io.cloudevents.core.format.contentType
import io.cloudevents.core.provider.EventFormatProvider
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.cloudEvent
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

data class MyCloudEventData(val value: Int) : CloudEventData {
    override fun toBytes() = value.toString().toByteArray()

    companion object {
        fun fromStringBytes(bytes: ByteArray) = MyCloudEventData(Integer.valueOf(String(bytes)))
    }
}

@ExtendWith(ApprovalTest::class)
class CustomFormatTest {

    @Test
    fun `can roundtrip custom format`(approver: Approver) {
        val format = CSVFormat()
        EventFormatProvider.getInstance().registerFormat(format)

        val lens = Body.cloudEvent(format.contentType()).toLens()

        val data = MyCloudEventData(123)
        val originalEvent = CloudEventBuilder.v1()
            .withId("123")
            .withType("type")
            .withDataContentType(ContentType.OCTET_STREAM)
            .withDataSchema(Uri.of("http4k"))
            .withSource(Uri.of("http4k"))
            .withSubject("subject")
            .withTime(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")))
            .withData(data)
            .build()

        val withData = Request(GET, "").with(lens of originalEvent)

        approver.assertApproved(withData.toMessage().replace("\r",""))

        val bytes = (lens(withData).data as BytesCloudEventData).toBytes()

        assertThat(MyCloudEventData.fromStringBytes(bytes), equalTo(data))
    }
}
