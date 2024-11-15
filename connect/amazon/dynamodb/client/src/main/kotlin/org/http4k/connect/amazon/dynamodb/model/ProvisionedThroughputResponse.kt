package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ProvisionedThroughputResponse(
    val LastDecreaseDateTime: Timestamp? = null,
    val LastIncreaseDateTime: Timestamp? = null,
    val NumberOfDecreasesToday: Long? = null,
    val ReadCapacityUnits: Long? = null,
    val WriteCapacityUnits: Long? = null
)
