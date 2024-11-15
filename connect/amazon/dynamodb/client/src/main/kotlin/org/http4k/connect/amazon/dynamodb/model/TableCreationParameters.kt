package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TableCreationParameters(
    val AttributeDefinitions: List<AttributeDefinition>,
    val KeySchema: List<KeySchema>,
    val TableName: TableName,
    val BillingMode: BillingMode? = null,
    val GlobalSecondaryIndexes: List<GlobalSecondaryIndex>? = null,
    val ProvisionedThroughput: ProvisionedThroughput? = null,
    val SSESpecification: SSESpecification? = null,
)

