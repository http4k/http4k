package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.AwsLambdaMoshi.asA
import org.http4k.format.AwsLambdaMoshi.asFormatString
import org.http4k.lens.Header
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class AwsLambdaMoshiTest {

    @Test
    fun `scheduled event`(approver: Approver) {
        approver.assertRoundtrips(ScheduledEvent().apply {
            id = "id"
            detailType = "detail"
            source = "source"
            account = "account"
            time = DateTime(0, DateTimeZone.UTC)
            region = "region"
            resources = listOf("resources")
            detail = mapOf("detailName" to "detailValue")
        })
    }

    private inline fun <reified T : Any> Approver.assertRoundtrips(input: T) {
        val asString = asFormatString(input)
        assertApproved(
            Response(Status.OK)
                .with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
                .body(asString)
        )
        assertThat(asA<T>(asString).toString(), equalTo(input.toString()))
    }
}
