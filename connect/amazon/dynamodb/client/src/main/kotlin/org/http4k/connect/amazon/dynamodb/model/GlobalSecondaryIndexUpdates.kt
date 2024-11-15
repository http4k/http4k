package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class GlobalSecondaryIndexUpdates(
    val Create: GlobalSecondaryIndexCreate? = null,
    val Delete: GlobalSecondaryIndexDelete? = null,
    val Update: GlobalSecondaryIndexUpdate? = null
)
