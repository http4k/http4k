package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.ItemResult
import org.http4k.connect.amazon.dynamodb.model.ParameterizedStatement
import org.http4k.connect.amazon.dynamodb.model.toItem
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ExecuteTransaction(
    val TransactStatements: List<ParameterizedStatement>,
    val ClientRequestToken: String? = null
) :
    DynamoDbAction<ExecutedTransaction>(ExecutedTransaction::class, DynamoDbMoshi)

@JsonSerializable
data class ExecutedTransaction(internal val Responses: List<ItemResult>) {
    val responses = Responses.map(ItemResult::toItem)
}
