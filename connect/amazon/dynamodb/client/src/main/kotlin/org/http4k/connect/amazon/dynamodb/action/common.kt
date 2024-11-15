package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.amazon.dynamodb.model.ConsumedCapacity
import org.http4k.connect.amazon.dynamodb.model.ItemCollectionMetrics
import org.http4k.connect.amazon.dynamodb.model.ItemResult
import org.http4k.connect.amazon.dynamodb.model.TableDescription
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TableDescriptionResponse(
    val TableDescription: TableDescription
)

@JsonSerializable
data class ModifiedItem(
    val Attributes: ItemResult? = null,
    val ConsumedCapacity: ConsumedCapacity? = null,
    val ItemCollectionMetrics: ItemCollectionMetrics? = null
)
