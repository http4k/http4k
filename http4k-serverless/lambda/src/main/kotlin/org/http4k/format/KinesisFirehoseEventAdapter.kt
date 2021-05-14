package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.KinesisFirehoseEvent
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.nio.ByteBuffer.wrap
import java.util.Base64.getDecoder
import java.util.Base64.getEncoder

object KinesisFirehoseEventAdapter : JsonAdapter<KinesisFirehoseEvent>() {
    @FromJson
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            obj(::KinesisFirehoseEvent) {
                when (it) {
                    "invocationId" -> invocationId = nextString()
                    "deliveryStreamArn" -> deliveryStreamArn = nextString()
                    "region" -> region = nextString()
                    "records" -> records = list(KinesisFirehoseEvent::Record) {
                        when (it) {
                            "data" -> data = wrap(getDecoder().decode(nextString()))
                            "recordId" -> recordId = nextString()
                            "approximateArrivalEpoch" -> approximateArrivalEpoch = nextLong()
                            "approximateArrivalTimestamp" -> approximateArrivalTimestamp = nextLong()
                            "kinesisRecordMetadata" -> kinesisRecordMetadata = stringMap()
                            else -> skipValue()
                        }
                    }
                    else -> skipValue()
                }
            }
        }

    @ToJson
    override fun toJson(writer: JsonWriter, event: KinesisFirehoseEvent?) {
        with(writer) {
            obj(event) {
                string("invocationId", invocationId)
                string("deliveryStreamArn", deliveryStreamArn)
                string("region", region)
                list("records", records) {
                    obj(this) {
                        string("data", getEncoder().encodeToString(data.array()))
                        string("recordId", recordId)
                        number("approximateArrivalEpoch", approximateArrivalEpoch)
                        number("approximateArrivalTimestamp", approximateArrivalTimestamp)
                        obj("kinesisRecordMetadata", kinesisRecordMetadata)
                    }
                }
            }
        }
    }
}
