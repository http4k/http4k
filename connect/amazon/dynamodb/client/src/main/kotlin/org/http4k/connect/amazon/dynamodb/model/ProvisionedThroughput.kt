package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ProvisionedThroughput(
    val ReadCapacityUnits: Long,
    val WriteCapacityUnits: Long
)
