package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.AwsLambdaMoshi.asA
import org.http4k.format.AwsLambdaMoshi.asFormatString
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class AwsLambdaMoshiTest {

    @Test
    fun `scheduled event`(approver: Approver) {
        approver.assertRoundtrips(ScheduledEvent().apply {
            id = "id"
            detailType = "detail"
            source = "source"
            account = "account"
            time = DateTime(0)
            region = "region"
            resources = listOf("resources")
            detail = mapOf("detailName" to "detailValue")
        })
    }

    private fun Approver.assertRoundtrips(input: Any) {
        val asString = asFormatString(input)
        assertApproved(asString)
        assertThat(asA(asString), equalTo(input))
    }
}
