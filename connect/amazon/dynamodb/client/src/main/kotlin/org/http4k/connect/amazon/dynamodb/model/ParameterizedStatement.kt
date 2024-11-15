package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ParameterizedStatement(
    val Statement: String,
    val parameters: List<AttributeValue>? = null
)
