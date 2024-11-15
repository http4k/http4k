package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.AttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.BillingMode
import org.http4k.connect.amazon.dynamodb.model.GlobalSecondaryIndex
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.LocalSecondaryIndex
import org.http4k.connect.amazon.dynamodb.model.ProvisionedThroughput
import org.http4k.connect.amazon.dynamodb.model.SSESpecification
import org.http4k.connect.amazon.dynamodb.model.StreamSpecification
import org.http4k.connect.amazon.dynamodb.model.TableName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class CreateTable(
    val TableName: TableName,
    val KeySchema: List<KeySchema>,
    val AttributeDefinitions: List<AttributeDefinition>,
    val GlobalSecondaryIndexes: List<GlobalSecondaryIndex>? = null,
    val LocalSecondaryIndexes: List<LocalSecondaryIndex>? = null,
    val Tags: List<Tag>? = null,
    val BillingMode: BillingMode? = null,
    val ProvisionedThroughput: ProvisionedThroughput? = null,
    val SSESpecification: SSESpecification? = null,
    val StreamSpecification: StreamSpecification? = null
) : DynamoDbAction<TableDescriptionResponse>(TableDescriptionResponse::class, DynamoDbMoshi)

