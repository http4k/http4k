package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.Record
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.nio.ByteBuffer
import java.util.Base64
import java.util.Date

object KinesisEventAdapter : JsonAdapter<KinesisEvent>() {
    @FromJson
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            obj(::KinesisEvent) {
                when (it) {
                    "records" -> records = list(::KinesisEventRecord) {
                        when (it) {
                            "eventSource" -> eventSource = nextString()
                            "eventID" -> eventID = nextString()
                            "invokeIdentityArn" -> invokeIdentityArn = nextString()
                            "eventName" -> eventName = nextString()
                            "eventVersion" -> eventVersion = nextString()
                            "eventSourceARN" -> eventSourceARN = nextString()
                            "awsRegion" -> awsRegion = nextString()
                            "kinesis" -> kinesis = obj(::Record) {
                                when (it) {
                                    "kinesisSchemaVersion" -> kinesisSchemaVersion = nextString()
                                    "encryptionType" -> encryptionType = nextString()
                                    "partitionKey" -> partitionKey = nextString()
                                    "sequenceNumber" -> sequenceNumber = nextString()
                                    "approximateArrivalTimestamp" -> approximateArrivalTimestamp = Date(nextLong())
                                    "data" -> data = ByteBuffer.wrap(Base64.getDecoder().decode(nextString()))
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
    override fun toJson(writer: JsonWriter, event: KinesisEvent?) {
        with(writer) {
            obj(event) {
                list("records", records) {
                    obj(this) {
                        string("eventSource", eventSource)
                        string("eventID", eventID)
                        string("invokeIdentityArn", invokeIdentityArn)
                        string("eventName", eventName)
                        string("eventVersion", eventVersion)
                        string("eventSourceARN", eventSourceARN)
                        string("awsRegion", awsRegion)
                        obj("kinesis", kinesis) {
                            string("kinesisSchemaVersion", kinesisSchemaVersion)
                            string("encryptionType", encryptionType)
                            string("partitionKey", partitionKey)
                            string("sequenceNumber", sequenceNumber)
                            number("approximateArrivalTimestamp", approximateArrivalTimestamp?.time)
                            string("data", Base64.getEncoder().encodeToString(data.array()))
                        }
                    }
                }
            }
        }
    }
}
