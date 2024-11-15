package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.KMSKeyId
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ReplicaUpdate(
    val GlobalSecondaryIndexes: List<GlobalSecondaryIndexesUpdate>? = null,
    val KMSMasterKeyId: KMSKeyId? = null,
    val ProvisionedThroughputOverride: ProvisionedThroughputOverride? = null,
    val RegionName: String? = null
)
