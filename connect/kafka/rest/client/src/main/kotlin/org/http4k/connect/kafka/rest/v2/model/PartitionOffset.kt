package org.http4k.connect.kafka.rest.v2.model

import org.http4k.connect.kafka.rest.model.Offset
import org.http4k.connect.kafka.rest.model.PartitionId
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class PartitionOffset(
    val partition: PartitionId,
    val offset: Offset
)
