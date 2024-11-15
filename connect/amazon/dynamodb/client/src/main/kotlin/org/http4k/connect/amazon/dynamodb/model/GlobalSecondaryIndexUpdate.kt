package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class GlobalSecondaryIndexUpdate(
    val IndexName: IndexName? = null,
    val ProvisionedThroughput: ProvisionedThroughput? = null
)
