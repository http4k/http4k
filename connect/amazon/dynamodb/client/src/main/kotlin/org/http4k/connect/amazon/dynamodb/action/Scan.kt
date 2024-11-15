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
data class Scan(
    val TableName: TableName,
    val FilterExpression: String? = null,
    val ProjectionExpression: String? = null,
    val ExpressionAttributeNames: TokensToNames? = null,
    val ExpressionAttributeValues: TokensToValues? = null,
    val ExclusiveStartKey: Key? = null,
    val IndexName: IndexName? = null,
    override val Limit: Int? = null,
    val ConsistentRead: Boolean? = null,
    val Segment: Int? = null,
    val Select: Select? = null,
    val TotalSegments: Int? = null,
    val ReturnConsumedCapacity: ReturnConsumedCapacity? = null,
) : DynamoDbPagedAction<ScanResponse, Scan>(kClass()) {
    override fun next(token: Key) = copy(ExclusiveStartKey = token)
}

@JsonSerializable
data class ScanResponse(
    val ConsumedCapacity: ConsumedCapacity? = null,
    val Count: Int = 0,
    val LastEvaluatedKey: Key? = null,
    val ScannedCount: Int = 0,
    internal val Items: List<ItemResult>? = null
) : Paged<Key, Item> {
    override val items = Items?.map(ItemResult::toItem) ?: emptyList()
    override fun token() = LastEvaluatedKey
}
