package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ReqStatement(
    val Statement: String,
    val ConsistentRead: Boolean? = null,
    val Parameters: List<AttributeValue>? = null
)
