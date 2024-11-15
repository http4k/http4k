package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class BatchStatementError(
    val Code: ErrorCode? = null,
    val Message: String? = null
)
