package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class DynamoDataType {
    B, BOOL, BS, L, M, N, NS, NULL, S, SS
}
