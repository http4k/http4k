package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Paged
import org.http4k.connect.amazon.dynamodb.model.ConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.ItemResult
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.ReturnConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.Select
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.TokensToNames
import org.http4k.connect.amazon.dynamodb.model.TokensToValues
import org.http4k.connect.amazon.dynamodb.model.toItem
import org.http4k.connect.kClass
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class Query(
    val TableName: TableName,
    val KeyConditionExpression: String? = null,
    val FilterExpression: String? = null,
    val ProjectionExpression: String? = null,
    val ExpressionAttributeNames: TokensToNames? = null,
    val ExpressionAttributeValues: TokensToValues? = null,
    val IndexName: IndexName? = null,
    val Select: Select? = null,
    val ConsistentRead: Boolean? = null,
    val ExclusiveStartKey: Key? = null,
    override val Limit: Int? = null,
    val ReturnConsumedCapacity: ReturnConsumedCapacity? = null,
    val ScanIndexForward: Boolean? = null,
) : DynamoDbPagedAction<QueryResponse, Query>(kClass()) {
    override fun next(token: Key) = copy(ExclusiveStartKey = token)
}

@JsonSerializable
data class QueryResponse(
    internal val Items: List<ItemResult>? = null,
    val ConsumedCapacity: ConsumedCapacity? = null,
    val Count: Int = 0,
    val LastEvaluatedKey: Key? = null,
    val ScannedCount: Int = 0
) : Paged<Key, Item> {
    override val items = Items?.map(ItemResult::toItem) ?: emptyList()
    override fun token() = LastEvaluatedKey
}
