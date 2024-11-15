package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class GlobalSecondaryIndexReplica(
    val IndexName: IndexName? = null,
    val ProvisionedThroughputOverride: ProvisionedThroughputOverride? = null
)
