package org.http4k.format

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity
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
                        val map = mutableMapOf<String, Any>()
                        while (hasNext()) {
                            beginObject()
                            val name = nextName()
                            map[name] = when (name) {
                                "awsRegion" -> nextString()
                                "eventName" -> nextString()
                                "eventSource" -> nextString()
                                "eventTime" -> nextString()
                                "eventVersion" -> nextString()
                                "requestParameters" -> requestParameters()
                                "responseElements" -> responseElements()
                                "s3" -> s3()
                                "userIdentity" -> userIdentity()
                                else -> error("unknown key")
                            }
                            records += S3EventNotificationRecord(
                                map["awsRegion"] as? String,
                                map["eventName"] as? String,
                                map["eventSource"] as? String,
                                map["eventTime"] as? String,
                                map["eventVersion"] as? String,
                                map["requestParameters"] as? RequestParametersEntity,
                                map["responseElements"] as? ResponseElementsEntity,
                                map["s3"] as? S3Entity,
                                map["userIdentity"] as? UserIdentityEntity,
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

    private fun JsonReader.userIdentity() = obj({ UserIdentityEntity(it["principalId"] as? String) }) {
        when (it) {
            "principalId" -> nextString()
            else -> error("unknown key")
        }
    }

    private fun JsonReader.s3() = obj(
        {
            S3Entity(
                it["configurationId"] as? String,
                it["bucket"] as? S3BucketEntity,
                it["object"] as? S3ObjectEntity,
                it["s3SchemaVersion"] as? String
            )
        }
    ) {
        when (it) {
            "configurationId" -> nextString()
            "bucket" -> bucket()
            "object" -> `object`()
            "s3SchemaVersion" -> nextString()
            else -> error("unknown key")
        }
    }

    private fun JsonReader.`object`() =
        obj({
            S3ObjectEntity(
                it["key"] as? String,
                it["size"] as? Long,
                it["eTag"] as? String,
                it["versionId"] as? String,
                it["sequencer"] as? String,
            )
        }) {
            when (it) {
                "key" -> nextString()
                "size" -> nextLong()
                "eTag" -> nextString()
                "versionId" -> nextString()
                "sequencer" -> nextString()
                else -> error("unknown key")
            }
        }

    private fun JsonReader.bucket() =
        obj({
            S3BucketEntity(
                it["name"] as? String,
                it["ownerIdentity"] as? UserIdentityEntity,
                it["arn"] as? String,
            )
        }) {
            when (it) {
                "name" -> nextString()
                "ownerIdentity" -> userIdentity()
                "arn" -> nextString()
                else -> error("unknown key")
            }
        }


    private fun JsonReader.requestParameters() =
        obj({ RequestParametersEntity(it["sourceIPAddress"] as? String) }) {
            when (it) {
                "sourceIPAddress" -> nextString()
                else -> error("unknown key")
            }
        }

    private fun JsonReader.responseElements() =
        obj({ ResponseElementsEntity(it["xAmzId2"] as? String, it["xAmzRequestId"] as? String) }) {
            when (it) {
                "xAmzId2" -> nextString()
                "xAmzRequestId" -> nextString()
                else -> error("unknown key")
            }
        }

    fun <T> JsonReader.obj(build: (Map<String, Any>) -> T, item: (String) -> Any): T {
        beginObject()
        val map = mutableMapOf<String, Any>()
        while (hasNext()) map[nextName()] = item(nextName())
        return build(map).also { endObject() }
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
