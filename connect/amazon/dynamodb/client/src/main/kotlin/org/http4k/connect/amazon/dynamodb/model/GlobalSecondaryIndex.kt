package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class GlobalSecondaryIndex(
    val IndexName: IndexName,
    val KeySchema: List<KeySchema>,
    val Projection: Projection,
    val ProvisionedThroughput: ProvisionedThroughput? = null
)
