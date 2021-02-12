package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.core.builder.withSource
import io.cloudevents.core.data.BytesCloudEventData
import io.cloudevents.core.format.contentType
import io.cloudevents.core.provider.EventFormatProvider
import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.MyCloudEventData
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class CustomFormatTest {

    @Test
    fun `can roundtrip custom format`(approver: Approver) {
        val format = CSVFormat()
        EventFormatProvider.getInstance().registerFormat(format)

        val lens = Body.cloudEvent(format.contentType()).toLens()

        val data = MyCloudEventData(123)
        val originalEvent = CloudEventBuilder.v03()
            .withId("123")
            .withType("type")
            .withSource(Uri.of("http4k"))
            .withData(data)
            .build()

        val withData = Request(GET, "").with(lens of originalEvent)

        approver.assertApproved(withData)

        val bytes = (lens(withData).data as BytesCloudEventData).toBytes()

        assertThat(MyCloudEventData.fromStringBytes(bytes), equalTo(data))
    }
}
