package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.ItemResult
import org.http4k.connect.amazon.dynamodb.model.toItem
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ExecuteStatement(
    val Statement: String,
    val Parameters: List<AttributeValue>? = null,
    val ConsistentRead: Boolean? = null,
    val NextToken: String? = null
) :
    DynamoDbAction<ExecutedStatement>(ExecutedStatement::class, DynamoDbMoshi)


@JsonSerializable
data class ExecutedStatement(internal val Items: List<ItemResult>) {
    val items = Items.map(ItemResult::toItem)
}
