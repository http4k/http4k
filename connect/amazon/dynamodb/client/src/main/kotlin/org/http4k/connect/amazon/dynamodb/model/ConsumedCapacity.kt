package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ConsumedCapacity(
    val TableName: TableName? = null,
    val CapacityUnits: Double? = null,
    val GlobalSecondaryIndexes: Map<String, Capacity>? = null,
    val LocalSecondaryIndexes: Map<String, Capacity>? = null,
    val ReadCapacityUnits: Double? = null,
    val Table: Capacity? = null,
    val WriteCapacityUnits: Double? = null
)
