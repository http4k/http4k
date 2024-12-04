package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class LocalSecondaryIndex(
    val IndexName: IndexName,
    val KeySchema: List<KeySchema>,
    val Projection: Projection
)
