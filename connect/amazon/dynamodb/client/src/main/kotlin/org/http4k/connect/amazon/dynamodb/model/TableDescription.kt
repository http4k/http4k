package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TableDescription(
    val ArchivalSummary: ArchivalSummary? = null,
    val AttributeDefinitions: List<AttributeDefinition>? = null,
    val BillingModeSummary: BillingModeSummary? = null,
    val CreationDateTime: Timestamp? = null,
    val GlobalSecondaryIndexes: List<GlobalSecondaryIndexResponse>? = null,
    val GlobalTableVersion: String? = null,
    val ItemCount: Long? = null,
    val KeySchema: List<KeySchema>? = null,
    val LatestStreamArn: ARN? = null,
    val LatestStreamLabel: String? = null,
    val LocalSecondaryIndexes: List<LocalSecondaryIndexResponse>? = null,
    val ProvisionedThroughput: ProvisionedThroughputResponse? = null,
    val Replicas: List<Replica>? = null,
    val RestoreSummary: RestoreSummary? = null,
    val SSEDescription: SSEDescription? = null,
    val StreamSpecification: StreamSpecification? = null,
    val TableArn: ARN? = null,
    val TableId: String? = null,
    val TableName: TableName? = null,
    val TableSizeBytes: Long? = null,
    val TableStatus: TableStatus? = null
)
