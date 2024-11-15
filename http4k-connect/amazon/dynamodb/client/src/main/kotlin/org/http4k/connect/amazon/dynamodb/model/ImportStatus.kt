package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class ImportStatus {
    IN_PROGRESS, COMPLETED, CANCELLING, CANCELLED, FAILED
}

