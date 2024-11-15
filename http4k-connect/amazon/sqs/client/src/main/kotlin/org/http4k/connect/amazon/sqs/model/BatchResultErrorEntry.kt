package org.http4k.connect.amazon.sqs.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class BatchResultErrorEntry(
    val Code: String,
    val Id: String,
    val SenderFault: Boolean,
    val Message: String? =null
)
