package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.amazonaws.services.lambda.runtime.events.SNSEvent.MessageAttribute
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object SNSEventAdapter : JsonAdapter<SNSEvent>() {
    @FromJson
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            obj(::SNSEvent) {
                when (it) {
                    "Records" -> records = list(::SNSRecord) {
                        when (it) {
                            "eventSource" -> eventSource = nextString()
                            "eventSubscriptionArn" -> eventSubscriptionArn = nextString()
                            "eventVersion" -> eventVersion = nextString()
                            "Sns" -> setSns(obj(::SNS) {
                                when (it) {
                                    "signingCertUrl" -> signingCertUrl = nextString()
                                    "messageId" -> messageId = nextString()
                                    "message" -> message = nextString()
                                    "subject" -> subject = nextString()
                                    "unsubscribeUrl" -> unsubscribeUrl = nextString()
                                    "type" -> type = nextString()
                                    "signatureVersion" -> signatureVersion = nextString()
                                    "signature" -> signature = nextString()
                                    "timestamp" -> timestamp = DateTime.parse(nextString())
                                    "topicArn" -> topicArn = nextString()
                                    "messageAttributes" -> messageAttributes = map {
                                        obj(::MessageAttribute) {
                                            when (it) {
                                                "type" -> type = nextString()
                                                "value" -> value = nextString()
                                                else -> skipValue()
                                            }
                                        }
                                    }
                                    else -> skipValue()
                                }
                            })
                            else -> skipValue()
                        }
                    }
                    else -> skipValue()
                }
            }
        }

    @ToJson
    override fun toJson(writer: JsonWriter, event: SNSEvent?) {
        with(writer) {
            obj(event) {
                list("Records", records) {
                    obj(this) {
                        string("eventSource", eventSource)
                        string("eventSubscriptionArn", eventSubscriptionArn)
                        string("eventVersion", eventVersion)
                        obj("Sns", sns) {
                            string("signingCertUrl", signingCertUrl)
                            string("messageId", messageId)
                            string("message", message)
                            string("subject", subject)
                            string("unsubscribeUrl", unsubscribeUrl)
                            string("type", type)
                            string("signatureVersion", signatureVersion)
                            string("signature", signature)
                            string("topicArn", topicArn)
                            string("timestamp", timestamp?.let { ISODateTimeFormat.dateTime().print(it) })
                            obj("messageAttributes", messageAttributes) {
                                forEach {
                                    obj(it.key, it.value) {
                                        string("type", type)
                                        string("value", value)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
