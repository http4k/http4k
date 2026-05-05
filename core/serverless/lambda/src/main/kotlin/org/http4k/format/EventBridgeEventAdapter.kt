package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.EventBridgeEvent
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object EventBridgeEventAdapter : TypedJsonAdapterFactory<EventBridgeEvent>(EventBridgeEvent::class.java) {
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            obj(::EventBridgeEvent) {
                when (it) {
                    "id" -> id = nextString()
                    "detail-type" -> detailType = nextString()
                    "source" -> source = nextString()
                    "account" -> account = nextString()
                    "time" -> time = DateTime.parse(nextString())
                    "region" -> region = nextString()
                    "resources" -> resources = stringList()
                    "detail" -> detail = stringMap() ?: error("Missing detail (required)")
                    else -> skipValue()
                }
            }
        }

    override fun toJson(writer: JsonWriter, event: EventBridgeEvent?) {
        with(writer) {
            obj(event) {
                string("id", id)
                string("detail-type", detailType)
                string("source", source)
                string("account", account)
                string("time", time?.let { ISODateTimeFormat.dateTime().print(it) })
                string("region", region)
                list("resources", resources)
                obj("detail", detail)
            }
        }
    }
}

