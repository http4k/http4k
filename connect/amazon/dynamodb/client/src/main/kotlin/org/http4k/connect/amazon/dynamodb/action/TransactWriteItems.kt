package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.ConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.ItemCollectionMetrics
import org.http4k.connect.amazon.dynamodb.model.ReturnConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.ReturnItemCollectionMetrics
import org.http4k.connect.amazon.dynamodb.model.TransactWriteItem
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class TransactWriteItems(
    val TransactItems: List<TransactWriteItem>,
    val ClientRequestToken: String? = null,
    val ReturnConsumedCapacity: ReturnConsumedCapacity? = null,
    val ReturnItemCollectionMetrics: ReturnItemCollectionMetrics? = null,
) : DynamoDbAction<ModifiedItems>(ModifiedItems::class, DynamoDbMoshi)

@JsonSerializable
data class ModifiedItems(
    val ConsumedCapacity: ConsumedCapacity? = null,
    val ItemCollectionMetrics: ItemCollectionMetrics? = null
)
