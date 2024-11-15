package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

object ScheduledEventAdapter : JsonAdapter<ScheduledEvent>() {
    @FromJson
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            obj(::ScheduledEvent) {
                when (it) {
                    "id" -> id = nextString()
                    "detail-type" -> detailType = nextString()
                    "source" -> source = nextString()
                    "account" -> account = nextString()
                    "time" -> time = DateTime.parse(nextString())
                    "region" -> region = nextString()
                    "resources" -> resources = stringList()
                    "detail" -> detail = stringMap()
                    else -> skipValue()
                }
            }
        }

    @ToJson
    override fun toJson(writer: JsonWriter, event: ScheduledEvent?) {
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
