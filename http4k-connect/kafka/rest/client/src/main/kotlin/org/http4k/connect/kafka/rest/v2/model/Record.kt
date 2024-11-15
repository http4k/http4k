package org.http4k.connect.kafka.rest.v2.model

import org.http4k.connect.kafka.rest.model.PartitionId
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Record<K : Any, out V : Any>(val key: K?, val `value`: V, val partition: PartitionId? = null)
