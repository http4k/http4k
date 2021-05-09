package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

object ScheduledEventAdapter : JsonAdapter<ScheduledEvent>() {
    @FromJson
    override fun fromJson(reader: JsonReader) = ScheduledEvent().apply {
        reader.beginObject()
        reader.endObject()
//            id = it["id"]?.toString()
//            detailType = it["detail-type"]?.toString()
//            source = it["source"]?.toString()
//            account = it["account"]?.toString()
//            time = it["time"]?.toString()?.let(DateTime::parse)
//            region = it["region"]?.toString()
//            resources = it["resources"] as List<String>?
//            detail = it["detail"] as Map<String, Any>?
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
                write("time", event.time?.toString())
                write("region", event.region)
                write("resources", event.resources)
                write("detail", event.detail)
                endObject()
            }
        }
    }
}
