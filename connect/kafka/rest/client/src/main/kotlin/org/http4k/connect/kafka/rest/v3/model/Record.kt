package org.http4k.connect.kafka.rest.v3.model

import org.http4k.connect.kafka.rest.model.PartitionId
import org.http4k.connect.kafka.rest.v3.action.Header
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class Record(
    val key: RecordData<*>? = null,
    val `value`: RecordData<*>? = null,
    val partition_id: PartitionId? = null,
    val headers: List<Header>? = null,
    val timestamp: Instant? = null
)
