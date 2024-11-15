package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class GetItemsResponseItem(val Item: ItemResult? = null)
