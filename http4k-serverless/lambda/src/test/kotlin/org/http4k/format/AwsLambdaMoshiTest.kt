package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.asByteBuffer
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.AwsLambdaMoshi.asA
import org.http4k.format.AwsLambdaMoshi.asFormatString
import org.http4k.lens.Header.CONTENT_TYPE
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

    @Test
    fun `sqs event`(approver: Approver) {
        approver.assertRoundtrips(SQSEvent().apply {
            records = listOf(
                SQSEvent.SQSMessage().apply {
                    messageId = "messageId"
                    receiptHandle = "receiptHandle"
                    body = "body"
                    md5OfBody = "md5OfBody"
                    md5OfMessageAttributes = "md5OfMessageAttributes"
                    eventSourceArn = "eventSourceArn"
                    eventSource = "eventSource"
                    awsRegion = "awsRegion"
                    attributes = mapOf("attr" to "attrvalue")
                    messageAttributes = mapOf("msgAttrName" to SQSEvent.MessageAttribute().apply {
                        stringListValues = listOf("stringListValues")
                        stringValue = "stringValue"
                        dataType = "datatype"
                        binaryValue = "binary".asByteBuffer()
                        binaryListValues = listOf("binaryListValues".asByteBuffer())
                    })
                }
            )
        })
    }
}

private inline fun <reified T : Any> Approver.assertRoundtrips(input: T) {
    val asString = asFormatString(input)
    println(asString)
    assertApproved(
        Response(OK)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body(asString)
    )
    assertThat(asA<T>(asString).toString(), equalTo(input.toString()))
}
