package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.ReqStatement
import org.http4k.connect.amazon.dynamodb.model.StatementResponse
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class BatchExecuteStatement(
    val Statements: List<ReqStatement>
) : DynamoDbAction<BatchStatements>(BatchStatements::class, DynamoDbMoshi)

@JsonSerializable
data class BatchStatements(val Responses: List<StatementResponse>)
