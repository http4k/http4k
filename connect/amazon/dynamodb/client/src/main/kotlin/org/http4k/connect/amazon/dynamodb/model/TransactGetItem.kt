package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
@ExposedCopyVisibility
data class TransactGetItem internal constructor(val Get: Map<String, Any?>) {
    companion object {
        fun Get(
            TableName: TableName,
            Key: Key,
            ProjectionExpression: String? = null,
            ExpressionAttributeNames: TokensToNames? = null
        ) = TransactGetItem(
            Get = mapOf(
                "TableName" to TableName,
                "Key" to Key,
                "ProjectionExpression" to ProjectionExpression,
                "ExpressionAttributeNames" to ExpressionAttributeNames
            )
        )
    }
}
