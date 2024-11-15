package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class StreamsEventResponse(
    val batchItemFailures: List<BatchItemFailure>,
)

@JsonSerializable
data class BatchItemFailure(
    val itemIdentifier: String,
)
