package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.ConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.GetItemsResponseItem
import org.http4k.connect.amazon.dynamodb.model.ItemCollectionMetrics
import org.http4k.connect.amazon.dynamodb.model.ReturnConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.TransactGetItem
import org.http4k.connect.amazon.dynamodb.model.toItem
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class TransactGetItems(
    val TransactItems: List<TransactGetItem>,
    val ReturnConsumedCapacity: ReturnConsumedCapacity? = null
) : DynamoDbAction<GetItemsResponse>(GetItemsResponse::class, DynamoDbMoshi)

@JsonSerializable
data class GetItemsResponse(
    internal val Responses: List<GetItemsResponseItem>,
    val ConsumedCapacity: ConsumedCapacity? = null,
    val ItemCollectionMetrics: ItemCollectionMetrics? = null
) {
    val responses = Responses.map { it.Item?.let { it.toItem() } }
}
