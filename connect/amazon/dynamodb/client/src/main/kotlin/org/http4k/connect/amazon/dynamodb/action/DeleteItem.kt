package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.ReturnConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.ReturnItemCollectionMetrics
import org.http4k.connect.amazon.dynamodb.model.ReturnValues
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.TokensToNames
import org.http4k.connect.amazon.dynamodb.model.TokensToValues
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DeleteItem(
    val TableName: TableName,
    val Key: Key,
    val ConditionExpression: String? = null,
    val ExpressionAttributeNames: TokensToNames? = null,
    val ExpressionAttributeValues: TokensToValues? = null,
    val ReturnConsumedCapacity: ReturnConsumedCapacity? = null,
    val ReturnItemCollectionMetrics: ReturnItemCollectionMetrics? = null,
    val ReturnValues: ReturnValues? = null
) : DynamoDbAction<ModifiedItem>(ModifiedItem::class, DynamoDbMoshi)
