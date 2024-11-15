package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class InputCompressionType {
    GZIP, ZSTD, NONE
}
