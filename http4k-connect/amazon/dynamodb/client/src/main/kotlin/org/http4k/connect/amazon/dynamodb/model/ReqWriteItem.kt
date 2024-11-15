package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
@ExposedCopyVisibility
data class ReqWriteItem internal constructor(
    val DeleteRequest: Map<String, Item>? = null,
    val PutRequest: Map<String, Item>? = null
) {
    companion object {
        fun Delete(Key: Key) = ReqWriteItem(DeleteRequest = mapOf("Key" to Key))
        fun Put(Item: Item) = ReqWriteItem(PutRequest = mapOf("Item" to Item))
    }
}
