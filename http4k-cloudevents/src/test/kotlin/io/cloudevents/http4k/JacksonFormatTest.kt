package io.cloudevents.http4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.cloudevents.CloudEventData
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.builder.withDataContentType
import io.cloudevents.core.builder.withDataSchema
import io.cloudevents.core.builder.withSource
import io.cloudevents.core.provider.EventFormatProvider
import io.cloudevents.with
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.format.cloudEventDataLens
import org.http4k.lens.cloudEvent
import org.http4k.testing.Approver
import org.http4k.testing.CloudEventsJsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

data class JacksonCloudEventData(val uri: Uri) : CloudEventData {
    override fun toBytes() = uri.toString().toByteArray()
}

@ExtendWith(CloudEventsJsonApprovalTest::class)
class JacksonFormatTest {

    @Test
    fun `can roundtrip custom format`(approver: Approver) {
        EventFormatProvider.getInstance().registerFormat(Jackson.cloudEventsFormat())

        val lens = Body.cloudEvent().toLens()

        val data = JacksonCloudEventData(Uri.of("foobar"))
        val dataLens = Jackson.cloudEventDataLens<JacksonCloudEventData>()

        val originalEvent = CloudEventBuilder.v1()
            .withId("123")
            .withType("type")
            .withDataContentType(ContentType.OCTET_STREAM)
            .withDataSchema(Uri.of("http4k"))
            .withSource(Uri.of("http4k"))
            .withSubject("subject")
            .withTime(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")))
            .build()

        val withData = originalEvent.with(dataLens of data)
        val req = Request(Method.GET, "").with(lens of withData)

        approver.assertApproved(req)

        val countEvent = lens(req)
        assertThat(dataLens(countEvent), equalTo(data))
    }

}
