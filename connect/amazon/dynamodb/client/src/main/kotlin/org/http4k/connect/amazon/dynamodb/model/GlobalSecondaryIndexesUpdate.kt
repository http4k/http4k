package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class GlobalSecondaryIndexesUpdate(
    val IndexName: IndexName? = null,
    val ProvisionedThroughputOverride: ProvisionedThroughputOverride? = null
)
