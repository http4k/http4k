package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent.AWSLogs
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

object CloudWatchLogsEventAdapter : JsonAdapter<CloudWatchLogsEvent>() {
    @FromJson
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            obj(::CloudWatchLogsEvent) {
                when (it) {
                    "awslogs" -> awsLogs = obj(::AWSLogs) {
                        when (it) {
                            "data" -> data = nextString()
                            else -> skipValue()
                        }
                    }
                }
            }
        }

    @ToJson
    override fun toJson(writer: JsonWriter, event: CloudWatchLogsEvent?) {
        with(writer) {
            obj(event) {
                obj("awslogs", awsLogs) {
                    string("data", data)
                }
            }
        }
    }
}
