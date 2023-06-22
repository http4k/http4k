package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent.MessageAttribute
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.http4k.base64DecodedByteBuffer
import org.http4k.base64Encode

object SQSEventAdapter : JsonAdapter<SQSEvent>() {
    @FromJson
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            obj(::SQSEvent) {
                when (it) {
                    "Records" -> records = list(::SQSMessage) {
                        when (it) {
                            "messageId" -> messageId = nextString()
                            "receiptHandle" -> receiptHandle = nextString()
                            "body" -> body = nextString()
                            "md5OfBody" -> md5OfBody = nextString()
                            "md5OfMessageAttributes" -> md5OfMessageAttributes = readJsonValue() as String?
                            "eventSourceArn" -> eventSourceArn = nextString()
                            "eventSource" -> eventSource = nextString()
                            "awsRegion" -> awsRegion = nextString()
                            "attributes" -> attributes = stringMap()
                            "messageAttributes" -> messageAttributes = map {
                                obj(::MessageAttribute) {
                                    when (it) {
                                        "binaryValue" -> binaryValue = nextString().base64DecodedByteBuffer()
                                        "binaryListValues" -> binaryListValues =
                                            stringList().map { it.base64DecodedByteBuffer() }
                                        "dataType" -> dataType = nextString()
                                        "stringValue" -> stringValue = nextString()
                                        "stringListValues" -> stringListValues = stringList()
                                        else -> skipValue()
                                    }
                                }
                            }
                            else -> skipValue()
                        }
                    }
                    else -> skipValue()
                }
            }
        }

    @ToJson
    override fun toJson(writer: JsonWriter, event: SQSEvent?) {
        with(writer) {
            obj(event) {
                list("Records", records) {
                    obj(this) {
                        string("messageId", messageId)
                        string("receiptHandle", receiptHandle)
                        string("body", body)
                        string("md5OfBody", md5OfBody)
                        string("md5OfMessageAttributes", md5OfMessageAttributes)
                        string("eventSourceArn", eventSourceArn)
                        string("eventSource", eventSource)
                        string("awsRegion", awsRegion)
                        obj("attributes", attributes)
                        obj("messageAttributes", messageAttributes) {
                            forEach {
                                obj(it.key, it.value) {
                                    list(
                                        "binaryListValues",
                                        binaryListValues?.map { it.base64Encode() })
                                    string(
                                        "binaryValue",
                                        binaryValue?.base64Encode()
                                    )
                                    string("dataType", dataType)
                                    list("stringListValues", stringListValues)
                                    string("stringValue", stringValue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
