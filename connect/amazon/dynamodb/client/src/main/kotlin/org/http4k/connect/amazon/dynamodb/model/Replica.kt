package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Replica(
    val GlobalSecondaryIndexes: List<GlobalSecondaryIndexReplica>? = null,
    val KMSMasterKeyId: KMSKeyId? = null,
    val ProvisionedThroughputOverride: ProvisionedThroughputOverride? = null,
    val RegionName: String? = null,
    val ReplicaInaccessibleDateTime: Timestamp? = null,
    val ReplicaStatus: ReplicaStatus? = null,
    val ReplicaStatusDescription: String? = null,
    val ReplicaStatusPercentProgress: String? = null
)
