package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent.AWSLogs
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

object CloudWatchLogsEventAdapter : TypedJsonAdapterFactory<CloudWatchLogsEvent>(CloudWatchLogsEvent::class.java) {
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
