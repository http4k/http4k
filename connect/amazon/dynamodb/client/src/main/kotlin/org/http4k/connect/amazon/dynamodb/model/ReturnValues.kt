package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class ReturnValues {
    NONE, ALL_OLD, UPDATED_OLD, ALL_NEW, UPDATED_NEW
}
