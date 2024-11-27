package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.core.model.Region
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ReplicaCreate(
    val GlobalSecondaryIndexes: List<GlobalSecondaryIndexesUpdate>? = null,
    val KMSMasterKeyId: KMSKeyId? = null,
    val ProvisionedThroughputOverride: ProvisionedThroughputOverride? = null,
    val RegionName: Region? = null
)
