package org.http4k.connect.kafka.rest.v2.model

import org.http4k.connect.kafka.rest.model.Offset
import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.model.Topic
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TopicRecord(
    val topic: Topic,
    val key: Any?,
    val `value`: Any?,
    val partition: PartitionId,
    val offset: Offset
)
