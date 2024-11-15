package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ItemCollectionMetrics(
    val ItemCollectionKey: Key? = null,
    val SizeEstimateRangeGB: List<Long>? = null
)
