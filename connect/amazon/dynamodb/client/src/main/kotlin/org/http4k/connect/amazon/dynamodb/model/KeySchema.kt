package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class KeySchema(
    val AttributeName: AttributeName,
    val KeyType: KeyType
) {
    companion object
}

fun KeySchema.Companion.compound(hash: AttributeName, range: AttributeName? = null) = listOfNotNull(
    KeySchema(hash, KeyType.HASH),
    range?.let { KeySchema(it, KeyType.RANGE) }
)
