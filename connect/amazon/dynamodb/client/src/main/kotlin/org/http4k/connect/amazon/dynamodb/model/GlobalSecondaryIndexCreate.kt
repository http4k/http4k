package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class GlobalSecondaryIndexCreate(
    val IndexName: IndexName? = null,
    val KeySchema: List<KeySchema>? = null,
    val Projection: Projection? = null,
    val ProvisionedThroughput: ProvisionedThroughput? = null
)
