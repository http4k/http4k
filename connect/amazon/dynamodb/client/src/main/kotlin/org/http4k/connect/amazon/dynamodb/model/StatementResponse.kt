package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class StatementResponse(
    val TableName: TableName? = null,
    val Error: BatchStatementError? = null,
    internal val Item: ItemResult? = null
) {
    val item = Item?.toItem() ?: emptyMap()
}
