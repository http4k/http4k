package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Identity
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamViewType
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.nio.ByteBuffer.wrap
import java.util.Base64
import java.util.Base64.getEncoder
import java.util.Date

object DynamodbEventAdapter : JsonAdapter<DynamodbEvent>() {
    @FromJson
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            obj(::DynamodbEvent) {
                when (it) {
                    "records" -> records = list(::DynamodbStreamRecord) {
                        when (it) {
                            "eventSourceARN" -> eventSourceARN = nextString()
                            "eventID" -> eventID = nextString()
                            "eventName" -> eventName = nextString()
                            "eventVersion" -> eventVersion = nextString()
                            "eventSource" -> eventSource = nextString()
                            "awsRegion" -> awsRegion = nextString()
                            "userIdentity" -> userIdentity = obj(::Identity) {
                                when (it) {
                                    "principalId" -> principalId = nextString()
                                    "type" -> type = nextString()
                                    else -> skipValue()
                                }
                            }
                            "dynamodb" -> dynamodb = obj(::StreamRecord) {
                                when (it) {
                                    "approximateCreationDateTime" -> approximateCreationDateTime = Date(nextLong())
                                    "sequenceNumber" -> sequenceNumber = nextString()
                                    "sizeBytes" -> sizeBytes = nextLong()
                                    "streamViewType" -> setStreamViewType(StreamViewType.valueOf(nextString()))
                                    "keys" -> keys = item()
                                    "newImage" -> newImage = item()
                                    "oldImage" -> oldImage = item()
                                    else -> skipValue()
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
    override fun toJson(writer: JsonWriter, event: DynamodbEvent?) {
        with(writer) {
            obj(event) {
                list("records", records) {
                    obj(this) {
                        string("eventSourceARN", eventSourceARN)
                        string("eventID", eventID)
                        string("eventName", eventName)
                        string("eventVersion", eventVersion)
                        string("eventSource", eventSource)
                        string("awsRegion", awsRegion)
                        obj("userIdentity", userIdentity) {
                            string("principalId", principalId)
                            string("type", type)
                        }
                        obj("dynamodb", dynamodb) {
                            number("approximateCreationDateTime", approximateCreationDateTime?.time)
                            string("sequenceNumber", sequenceNumber)
                            number("sizeBytes", sizeBytes)
                            string("streamViewType", streamViewType)
                            obj("keys", keys) { entries.forEach { item(it) } }
                            obj("newImage", newImage) { entries.forEach { item(it) } }
                            obj("oldImage", oldImage) { entries.forEach { item(it) } }
                        }
                    }
                }
            }
        }
    }

    private fun JsonWriter.item(it: Map.Entry<String, AttributeValue>) {
        obj(it.key, it.value) { attributeValue(this) }
    }

    private fun JsonWriter.attributeValue(attributeValue: AttributeValue) {
        string("B", attributeValue.b?.let { getEncoder().encodeToString(it.array()) })
        list("BS", attributeValue.bs?.map { getEncoder().encodeToString(it.array()) })
        boolean("BOOL", attributeValue.bool)
        list("L", attributeValue.l) {
            beginObject()
            attributeValue(this)
            endObject()
        }
        obj("M", attributeValue.m) { forEach { item(it) } }
        string("N", attributeValue.n)
        list("NS", attributeValue.ns)
        string("S", attributeValue.s)
        list("SS", attributeValue.ss)
    }

    private fun JsonReader.item(): Map<String, AttributeValue> =
        map { obj(::AttributeValue) { attributeValue(it, this) } }

    private fun JsonReader.attributeValue(name: String, attributeValue: AttributeValue) {
        when (name) {
            "B" -> attributeValue.b = wrap(Base64.getDecoder().decode(nextString()))
            "BS" -> attributeValue.setBS(stringList().map { wrap(Base64.getDecoder().decode(it)) })
            "BOOL" -> attributeValue.bool = nextBoolean()
            "L" -> attributeValue.setL(list { obj(::AttributeValue) { attributeValue(it, this) } })
            "M" -> attributeValue.m = item()
            "N" -> attributeValue.n = nextString()
            "NS" -> attributeValue.setNS(stringList())
            "S" -> attributeValue.s = nextString()
            "SS" -> attributeValue.setSS(stringList())
            else -> skipValue()
        }
    }
}
