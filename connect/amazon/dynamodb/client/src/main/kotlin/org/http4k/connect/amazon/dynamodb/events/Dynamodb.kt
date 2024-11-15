package org.http4k.connect.amazon.dynamodb.events

import org.http4k.connect.amazon.dynamodb.model.Item
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Dynamodb(
    val Keys: Item? = null,
    val NewImage: Item? = null,
    val OldImage: Item? = null,
    val SequenceNumber: String? = null,
    val SizeBytes: Long? = null,
    val StreamViewType: StreamViewType? = null
)
