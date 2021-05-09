package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

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
//                messageAttributes = mapOf("msgAttrName" to SQSEvent.MessageAttribute().apply {
//                    stringListValues = listOf("stringListValues")
//                    stringValue = "stringValue"
//                    dataType = "datatype"
//                    binaryValue = "binary".asByteBuffer()
//                    binaryListValues = listOf("binaryListValues".asByteBuffer())
//                })
                    endObject()
                }
                endArray()
                endObject()
            }
        }
    }
}
