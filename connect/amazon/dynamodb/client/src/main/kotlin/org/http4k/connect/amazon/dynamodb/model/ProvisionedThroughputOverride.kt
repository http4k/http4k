package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ProvisionedThroughputOverride(
    val ReadCapacityUnits: Long? = null
)
