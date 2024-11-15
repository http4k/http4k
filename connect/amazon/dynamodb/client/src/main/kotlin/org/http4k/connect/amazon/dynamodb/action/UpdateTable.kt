package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.AttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.BillingMode
import org.http4k.connect.amazon.dynamodb.model.GlobalSecondaryIndexUpdates
import org.http4k.connect.amazon.dynamodb.model.ProvisionedThroughput
import org.http4k.connect.amazon.dynamodb.model.ReplicaUpdates
import org.http4k.connect.amazon.dynamodb.model.SSESpecification
import org.http4k.connect.amazon.dynamodb.model.StreamSpecification
import org.http4k.connect.amazon.dynamodb.model.TableName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class UpdateTable(
    val TableName: TableName,
    val AttributeDefinitions: List<AttributeDefinition>? = null,
    val BillingMode: BillingMode? = null,
    val GlobalSecondaryIndexUpdates: List<GlobalSecondaryIndexUpdates>? = null,
    val ProvisionedThroughput: ProvisionedThroughput? = null,
    val ReplicaUpdates: List<ReplicaUpdates>? = null,
    val SSESpecification: SSESpecification? = null,
    val StreamSpecification: StreamSpecification? = null
) :
    DynamoDbAction<TableDescriptionResponse>(TableDescriptionResponse::class, DynamoDbMoshi)
