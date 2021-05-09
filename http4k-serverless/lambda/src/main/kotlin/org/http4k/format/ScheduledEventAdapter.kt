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
    override fun fromJson(reader: JsonReader) = ScheduledEvent().apply {
        with(reader) {
            beginObject()
            while (hasNext()) {
                when (nextName()) {
                    "id" -> id = nextString()
                    "detail-type" -> detailType = nextString()
                    "source" -> source = nextString()
                    "account" -> account = nextString()
                    "time" -> time = nextString()?.let(DateTime::parse)
                    "region" -> region = nextString()
                    "resources" -> resources = readStringList()
                    "detail" -> detail = readMap()
                    else -> skipValue()
                }
            }
            endObject()
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, event: ScheduledEvent?) {
        when (event) {
            null -> writer.nullValue()
            else -> with(writer) {
                beginObject()
                write("id", event.id)
                write("detail-type", event.detailType)
                write("source", event.source)
                write("account", event.account)
                write("time", event.time?.let { ISODateTimeFormat.dateTime().print(it) })
                write("region", event.region)
                write("resources", event.resources)
                write("detail", event.detail)
                endObject()
            }
        }
    }
}
