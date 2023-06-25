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
                            "EventSource" -> eventSource = nextString()
                            "EventSubscriptionArn" -> eventSubscriptionArn = nextString()
                            "EventVersion" -> eventVersion = nextString()
                            "Sns" -> setSns(obj(::SNS) {
                                when (it) {
                                    "SigningCertUrl" -> signingCertUrl = nextString()
                                    "MessageId" -> messageId = nextString()
                                    "Message" -> message = nextString()
                                    "Subject" -> subject = stringOrNull()
                                    "UnsubscribeUrl" -> unsubscribeUrl = nextString()
                                    "Type" -> type = nextString()
                                    "SignatureVersion" -> signatureVersion = nextString()
                                    "Signature" -> signature = nextString()
                                    "Timestamp" -> timestamp = DateTime.parse(nextString())
                                    "TopicArn" -> topicArn = nextString()
                                    "MessageAttributes" -> messageAttributes = map {
                                        obj(::MessageAttribute) {
                                            when (it) {
                                                "Type" -> type = nextString()
                                                "Value" -> value = nextString()
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
                        string("EventSource", eventSource)
                        string("EventSubscriptionArn", eventSubscriptionArn)
                        string("EventVersion", eventVersion)
                        obj("Sns", sns) {
                            string("SigningCertUrl", signingCertUrl)
                            string("MessageId", messageId)
                            string("Message", message)
                            string("Subject", subject)
                            string("UnsubscribeUrl", unsubscribeUrl)
                            string("Type", type)
                            string("SignatureVersion", signatureVersion)
                            string("Signature", signature)
                            string("TopicArn", topicArn)
                            string("Timestamp", timestamp?.let { ISODateTimeFormat.dateTime().print(it) })
                            obj("MessageAttributes", messageAttributes) {
                                forEach {
                                    obj(it.key, it.value) {
                                        string("Type", type)
                                        string("Value", value)
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
