package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.util.Base64.getEncoder

object SQSEventAdapter : JsonAdapter<SQSEvent>() {
    @FromJson
    override fun fromJson(reader: JsonReader) = SQSEvent().apply {
        with(reader) {
            beginObject()
            while (hasNext()) {
                when (nextName()) {
                    "records" -> records = run {
                        val recs = mutableListOf<SQSEvent.SQSMessage>()
                        beginArray()
                        while (hasNext()) {
                            beginObject()
                            when (nextName()) {
//                                        "messageId" -> messageId = ""
//                                        "receiptHandle" -> receiptHandle = ""
//                                        "body" -> body = ""
//                                        "md5OfBody" -> md5OfBody = ""
//                                        "md5OfMessageAttributes" -> md5OfMessageAttributes = ""
//                                        "eventSourceArn" -> eventSourceArn = ""
//                                        "eventSource" -> eventSource = ""
//                                        "awsRegion" -> awsRegion = ""
//                                        "attributes" -> attributes = ""
                                else -> skipValue()
                            }
                            endObject()
                        }
                        endArray()
                        recs
                    }
                    else -> skipValue()
                }
            }
            endObject()
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, event: SQSEvent?) {
        when (event) {
            null -> writer.nullValue()
            else -> with(writer) {
                beginObject()
                name("records")
                beginArray()
                event.records?.forEach {
                    beginObject()
                    write("messageId", it.messageId)
                    write("receiptHandle", it.receiptHandle)
                    write("body", it.body)
                    write("md5OfBody", it.md5OfBody)
                    write("md5OfMessageAttributes", it.md5OfMessageAttributes)
                    write("eventSourceArn", it.eventSourceArn)
                    write("eventSource", it.eventSource)
                    write("awsRegion", it.awsRegion)
                    write("attributes", it.attributes)
                    name("messageAttributes")
                    beginObject()
                    it.messageAttributes?.forEach {
                        name(it.key)
                        beginObject()
                        write(
                            "binaryListValues",
                            it.value.binaryListValues?.map { getEncoder().encodeToString(it.array()) })
                        write(
                            "binaryValue",
                            it.value.binaryValue?.let { getEncoder().encodeToString(it.array()) })
                        write("dataType", it.value.dataType)
                        write("stringListValues", it.value.stringListValues)
                        write("stringValue", it.value.stringValue)
                        endObject()
                    }
                    endObject()
                    endObject()
                }
                endArray()
                endObject()
            }
        }
    }
}
