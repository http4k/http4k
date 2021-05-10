package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
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
    fun `Dynamodb event`(approver: Approver) {
        approver.assertRoundtrips(DynamodbEvent().apply {
        })
    }

    @Test
    fun `Kinesis event`(approver: Approver) {
        approver.assertRoundtrips(KinesisEvent().apply {
        })
    }

    @Test
    fun `KinesisFirehose event`(approver: Approver) {
        approver.assertRoundtrips(KinesisFirehoseEvent().apply {
        })
    }

    @Test
    fun `S3 event`(approver: Approver) {
        approver.assertRoundtrips(S3Event().apply {
        })
    }

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
    fun `SNS event`(approver: Approver) {
        approver.assertRoundtrips(SNSEvent().apply {
        })
    }


    @Test
    fun `sqs event`(approver: Approver) {
        approver.assertRoundtrips(SQSEvent().apply {
            records = listOf(
                SQSMessage().apply {
                    messageId = "messageId"
                    receiptHandle = "receiptHandle"
                    body = "body"
                    md5OfBody = "md5OfBody"
                    md5OfMessageAttributes = "md5OfMessageAttributes"
                    eventSourceArn = "eventSourceArn"
                    eventSource = "eventSource"
                    awsRegion = "awsRegion"
                    attributes = mapOf("attr" to "attrvalue")
                    messageAttributes = mapOf("msgAttrName" to MessageAttribute().apply {
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
    assertApproved(
        Response(OK)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body(asString)
    )
//    println(input.toString())
//    println(asA<T>(asString).toString())
    assertThat(asA<T>(asString).toString(), equalTo(input.toString()))
}
