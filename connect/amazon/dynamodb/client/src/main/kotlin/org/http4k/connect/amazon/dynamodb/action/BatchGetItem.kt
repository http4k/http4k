package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.ConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.ReqGetItem
import org.http4k.connect.amazon.dynamodb.model.ReturnConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.TableName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class BatchGetItem(
    val RequestItems: Map<TableName, ReqGetItem>,
    val ReturnConsumedCapacity: ReturnConsumedCapacity? = null
) : DynamoDbAction<BatchGetItems>(BatchGetItems::class, DynamoDbMoshi)

@JsonSerializable
data class BatchGetItems(
    val Responses: Map<String, List<Item>>? = null,
    val UnprocessedKeys: Map<String, ReqGetItem>? = null,
    val ConsumedCapacity: List<ConsumedCapacity>? = null,
)
