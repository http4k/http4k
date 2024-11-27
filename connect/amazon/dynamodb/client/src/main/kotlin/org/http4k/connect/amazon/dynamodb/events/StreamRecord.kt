package org.http4k.connect.amazon.dynamodb.events

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class StreamRecord(
    val eventID: String? = null,
    val eventName: EventName? = null,
    val eventVersion: String? = null,
    val eventSource: String? = null,
    val awsRegion: Region? = null,
    val dynamodb: Dynamodb? = null,
    val eventSourceARN: ARN? = null
)
