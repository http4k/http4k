package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

object S3EventAdapter : JsonAdapter<S3Event>() {
    @FromJson
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            beginObject()
            val records = mutableListOf<S3EventNotificationRecord>()
            while (hasNext()) {
                when (nextName()) {
                    "records" -> {
                        beginArray()
                        val recordMap = mutableMapOf<String, Any>()
                        while (hasNext()) {
                            beginObject()
                            val name = nextName()
                            recordMap[name] = when (name) {
                                "awsRegion" -> nextString()
                                "eventName" -> nextString()
                                "eventSource" -> nextString()
                                "eventTime" -> nextString()
                                "eventVersion" -> nextString()
                                "requestParameters" -> {
                                    beginObject()
                                    nextName()
                                    RequestParametersEntity(nextString()).also { endObject() }
                                }
                                "responseElements" -> {
                                    val recordMap = mutableMapOf<String, Any>()
                                    beginObject()
                                    while (hasNext()) {
                                        recordMap[name] = when (name) {
                                            "xAmzId2" -> nextString()
                                            "xAmzRequestId" -> nextString()
                                            else -> error("unknown key")
                                        }
                                    }
                                    ResponseElementsEntity(
                                        recordMap["xAmzId2"] as? String,
                                        recordMap["xAmzRequestId"] as? String
                                    ).also { endObject() }
                                }
                                "s3" -> nextString()
                                "userIdentity" -> nextString()
                                else -> error("unknown key")
                            }
                            records += S3EventNotificationRecord(
                                recordMap["awsRegion"] as? String,
                                recordMap["eventName"] as? String,
                                recordMap["eventSource"] as? String,
                                recordMap["eventTime"] as? String,
                                recordMap["eventVersion"] as? String,
                                recordMap["requestParameters"] as? RequestParametersEntity,
                                recordMap["responseElements"] as? ResponseElementsEntity,
                                recordMap["s3"] as? S3Entity,
                                recordMap["userIdentity"] as? UserIdentityEntity,
                            )
                            endObject()
                        }
                        endArray()
                    }
                    else -> skipName()
                }
            }
            endObject()
            S3Event(records)
        }

    @ToJson
    override fun toJson(writer: JsonWriter, event: S3Event?) {
        with(writer) {
            obj(event) {
                list("records", records) {
                    obj(this) {
                        string("awsRegion", awsRegion)
                        string("eventName", eventName)
                        string("eventSource", eventSource)
                        string("eventVersion", eventVersion)
                        obj("requestParameters", requestParameters) {
                            string("sourceIPAddress", sourceIPAddress)
                        }
                        obj("responseElements", responseElements) {
                            string("xAmzId2", getxAmzId2())
                            string("xAmzRequestId", getxAmzRequestId())
                        }
                        obj("userIdentity", userIdentity) {
                            string("principalId", principalId)
                        }
                        obj("s3", s3) {
                            string("configurationId", configurationId)
                            string("s3SchemaVersion", s3SchemaVersion)
                            obj("bucket", bucket) {
                                string("arn", arn)
                                string("name", name)
                                obj("ownerIdentity", ownerIdentity) {
                                    string("principalId", principalId)
                                }
                            }
                            obj("object", `object`) {
                                string("key", key)
                                number("size", sizeAsLong)
                                string("eTag", geteTag())
                                string("versionId", versionId)
                                string("sequencer", sequencer)
                            }
                        }
                    }
                }
            }
        }
    }
}
