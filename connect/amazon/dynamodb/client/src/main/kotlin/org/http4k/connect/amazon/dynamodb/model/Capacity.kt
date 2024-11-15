package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Capacity(
    val CapacityUnits: Long? = null,
    val ReadCapacityUnits: Long? = null,
    val WriteCapacityUnits: Long? = null
)
