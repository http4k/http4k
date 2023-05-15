package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.builder.withDataSchema
import io.cloudevents.core.builder.withSource
import io.cloudevents.core.provider.EventFormatProvider
import io.cloudevents.http4k.cloudEventsFormat
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.Jackson.withData
import org.http4k.lens.cloudEvent
import org.http4k.testing.Approver
import org.http4k.testing.CloudEventsJsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

data class EventData(val uri: Uri)

@ExtendWith(CloudEventsJsonApprovalTest::class)
class JacksonFormatTest {

    @Test
    fun `can roundtrip custom format`(approver: Approver) {
        EventFormatProvider.getInstance().registerFormat(Jackson.cloudEventsFormat())

        val lens = Body.cloudEvent().toLens()

        val data = EventData(Uri.of("foobar"))

        val originalEvent = CloudEventBuilder.v1()
            .withId("123")
            .withType("type")
            .withDataSchema(Uri.of("http4k"))
            .withSource(Uri.of("http4k"))
            .withSubject("subject")
            .withTime(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")))
            .withData(data)
            .build()

        val req = Request(GET, "").with(lens of originalEvent)

        approver.assertApproved(req)

        val dataLens = Jackson.cloudEventDataLens<EventData>()

        assertThat(dataLens(lens(req)), equalTo(data))
    }

}
