package org.http4k.connect.amazon.dynamodb.events

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class DynamoDbEvent(
    val Records: List<StreamRecord>? = null
)
