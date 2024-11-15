package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Get(
    val TableName: TableName,
    val Key: Key,
    val ProjectionExpression: String? = null,
    val ExpressionAttributeNames: TokensToNames? = null
)
