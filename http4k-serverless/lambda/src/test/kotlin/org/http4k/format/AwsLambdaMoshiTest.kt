package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Identity
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamViewType.KEYS_ONLY
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.asByteBuffer
import org.http4k.base64DecodedByteBuffer
import org.http4k.base64EncodeArray
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
import org.joda.time.DateTimeZone.UTC
import org.joda.time.format.ISODateTimeFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Date

@ExtendWith(JsonApprovalTest::class)
class AwsLambdaMoshiTest {
    @Test
    fun `CloudWatchLogs event`(approver: Approver) {
        approver.assertRoundtrips(CloudWatchLogsEvent().apply {
            awsLogs = CloudWatchLogsEvent.AWSLogs().apply {
                data = "logsData"
            }
        })
    }

    @Test
    fun `SQSBatchResponse event`(approver: Approver) {
        approver.assertRoundtrips(SQSBatchResponse().apply {
            batchItemFailures = listOf(
                SQSBatchResponse.BatchItemFailure().apply {
                    itemIdentifier = "itemIdentifier"
                }
            )
        })
    }

    @Test
    fun `Dynamodb event`(approver: Approver) {
        approver.assertRoundtrips(DynamodbEvent().apply {
            records = listOf(DynamodbEvent.DynamodbStreamRecord().apply {
                eventSourceARN = "eventSourceARN"
                eventID = "eventID"
                eventName = "eventName"
                eventVersion = "eventVersion"
                eventSource = "eventSource"
                awsRegion = "awsRegion"
                userIdentity = Identity().apply {
                    principalId = "principalId"
                    type = "type"
                }
                dynamodb = StreamRecord().apply {
                    approximateCreationDateTime = Date(0)
                    sequenceNumber = "sequenceNumber"
                    sizeBytes = 123
                    setStreamViewType(KEYS_ONLY)
                    keys = item()
                    oldImage = item()
                    newImage = item()
                }
            })
        })
    }

    private fun item(): Map<String, AttributeValue> = mapOf("name" to AttributeValue().apply {
        val attributeValue = this
        attributeValue.b = "123".base64DecodedByteBuffer()
        attributeValue.setBS(listOf("123".base64DecodedByteBuffer()))
        attributeValue.bool = true
        attributeValue.setL(listOf(AttributeValue().apply { s = "123" }))
        attributeValue.m = mapOf()
        attributeValue.n = "123"
        attributeValue.setNS(listOf("123"))
        attributeValue.s = "123"
        attributeValue.setSS(listOf("123"))
    })

    @Test
    fun `Kinesis event`(approver: Approver) {
        approver.assertRoundtrips(KinesisEvent().apply {
            records = listOf(
                KinesisEvent.KinesisEventRecord().apply {
                    eventSource = "eventSource"
                    eventID = "eventID"
                    invokeIdentityArn = "invokeIdentityArn"
                    eventName = "eventName"
                    eventVersion = "eventVersion"
                    eventSourceARN = "eventSourceARN"
                    awsRegion = "awsRegion"
                    kinesis = KinesisEvent.Record().apply {
                        kinesisSchemaVersion = "kinesisSchemaVersion"
                        encryptionType = "encryptionType"
                        partitionKey = "partitionKey"
                        sequenceNumber = "sequenceNumber"
                        approximateArrivalTimestamp = Date(0)
                        data = "hello".toByteArray().base64EncodeArray().asByteBuffer()
                    }
                })
        })
    }

    @Test
    fun `KinesisFirehose event`(approver: Approver) {
        approver.assertRoundtrips(KinesisFirehoseEvent().apply {
            invocationId = "invocationId"
            deliveryStreamArn = "deliveryStreamArn"
            region = "region"
            records = listOf(
                KinesisFirehoseEvent.Record().apply {
                    data = "hello".toByteArray().base64EncodeArray().asByteBuffer()
                    recordId = "recordId"
                    approximateArrivalEpoch = 0
                    approximateArrivalTimestamp = 0
                    kinesisRecordMetadata = mapOf("hello" to "world")
                })
        })
    }

    @Test
    fun `S3 event`(approver: Approver) {
        approver.assertRoundtrips(
            S3Event(
                listOf(
                    S3EventNotificationRecord(
                        "awsRegion",
                        "eventName",
                        "eventSource",
                        ISODateTimeFormat.dateTime().print(DateTime(0, UTC)),
                        "eventVersion",
                        RequestParametersEntity("sourceIp"),
                        ResponseElementsEntity("xAmzId2", "xAmzRequestId"),
                        S3Entity(
                            "configurationId",
                            S3BucketEntity(
                                "name",
                                UserIdentityEntity("principalId"), "arn"
                            ),
                            S3ObjectEntity("key", 123, "eTag", "versionId", "sequence"),
                            "s3SchemaVersion"
                        ),
                        UserIdentityEntity("principalId")
                    )
                )
            )
        )
    }

    @Test
    fun `Scheduled event`(approver: Approver) {
        approver.assertRoundtrips(ScheduledEvent().apply {
            id = "id"
            detailType = "detail"
            source = "source"
            account = "account"
            time = DateTime(0, UTC)
            region = "region"
            resources = listOf("resources")
            detail = mapOf("detailName" to "detailValue")
        })
    }

    @Test
    fun `SNS event`(approver: Approver) {
        approver.assertRoundtrips(SNSEvent().apply {
            records = listOf(
                SNSEvent.SNSRecord().apply {
                    eventSource = "eventSource"
                    eventSubscriptionArn = "eventSubscriptionArn"
                    eventVersion = "eventVersion"
                    setSns(SNSEvent.SNS().apply {
                        signingCertUrl = "signingCertUrl"
                        messageId = "messageId"
                        message = "message"
                        subject = "subject"
                        unsubscribeUrl = "unsubscribeUrl"
                        type = "type"
                        signatureVersion = "type"
                        signature = "signature"
                        topicArn = "topicArn"
                        timestamp = DateTime(0, UTC)
                        messageAttributes = mapOf("msgAttrName" to SNSEvent.MessageAttribute().apply {
                            type = "type"
                            value = "value"
                        })
                    })
                }
            )
        })
    }

    @Test
    fun `SNS event - read null subject`() {
        val actual = javaClass.getResourceAsStream("AwsLambdaMoshiTest.SNS event - read null subject.approved")!!
            .reader().readText()
            .let { asA<SNSEvent>(it) }

        val expected = SNSEvent().apply {
            records = listOf(
                SNSEvent.SNSRecord().apply {
                    eventSource = "eventSource"
                    eventSubscriptionArn = "eventSubscriptionArn"
                    eventVersion = "eventVersion"
                    setSns(SNSEvent.SNS().apply {
                        signingCertUrl = "signingCertUrl"
                        messageId = "messageId"
                        message = "message"
                        subject = null
                        unsubscribeUrl = "unsubscribeUrl"
                        type = "type"
                        signatureVersion = "type"
                        signature = "signature"
                        topicArn = "topicArn"
                        timestamp = DateTime(0, UTC)
                        messageAttributes = mapOf("msgAttrName" to SNSEvent.MessageAttribute().apply {
                            type = "type"
                            value = "value"
                        })
                    })
                }
            )
        }

        assertThat(expected, equalTo(actual))
    }

    @Test
    fun `SQS event with no message attributes`() {
        assertNull(
            asA<SQSEvent>(
                """
                    {
                      "Records": [
                        {
                          "messageId": "messageId",
                          "receiptHandle": "receiptHandle",
                          "body": "body",
                          "md5OfBody": "md5OfBody",
                          "md5OfMessageAttributes": null,
                          "eventSourceArn": "eventSourceArn",
                          "eventSource": "eventSource",
                          "awsRegion": "awsRegion",
                          "attributes": {
                            "attr": "attrvalue"
                          },
                          "messageAttributes": { }
                        }
                      ]
                    }
                """.trimIndent()
            ).records[0].md5OfMessageAttributes
        )
    }

    @Test
    fun `SQS event with null message attributes`() {
        val json = """
            {
              "Records": [
                {
                  "messageId": "messageId",
                  "receiptHandle": "receiptHandle",
                  "body": "body",
                  "md5OfBody": "md5OfBody",
                  "md5OfMessageAttributes": "md5OfMessageAttributes",
                  "eventSourceArn": "eventSourceArn",
                  "eventSource": "eventSource",
                  "awsRegion": "awsRegion",
                  "attributes": {
                    "attr": "attrvalue"
                  },
                  "messageAttributes": {
                    "msgAttrName": {
                      "binaryValue": null,
                      "binaryListValues": null,
                      "dataType": "String",
                      "stringValue": "stringValue"
                    }
                  }
                }
              ]
            }
        """.trimIndent()
        val evt = asA<SQSEvent>(json)
        println(evt)
        assertNull(evt.records[0].messageAttributes["msgAttrName"]?.binaryValue)
        assertEquals("stringValue", evt.records[0].messageAttributes["msgAttrName"]?.stringValue)
    }

    @Test
    fun `SQS event`(approver: Approver) {
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
    assertThat(asFormatString(asA<T>(asString)), equalTo(asString))
}
