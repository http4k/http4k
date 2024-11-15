package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class GlobalSecondaryIndexDelete(
    val IndexName: IndexName? = null
)
