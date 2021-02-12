package org.http4k.lens

import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.builder.withSource
import io.cloudevents.core.format.contentType
import io.cloudevents.core.provider.EventFormatProvider
import org.http4k.cloudevents.CSVFormat
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class CustomFormatTest {

    @Test
    fun `can roundtrip custom format`(approver: Approver) {
        val format = CSVFormat()
        EventFormatProvider.getInstance().registerFormat(format)

        val lens = Body.cloudEvent(format.contentType()).toLens()

        val event = CloudEventBuilder.v03()
            .withId("123")
            .withType("type")
            .withSource(Uri.of("http4k"))
            .withData(MyCloudEventData(123))
            .build()

        approver.assertApproved(Request(GET, "").with(lens of event).toMessage())
    }
}
