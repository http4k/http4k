package org.http4k.connect.amazon.dynamodb

import org.http4k.connect.amazon.dynamodb.endpoints.key
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.TableDescription

data class DynamoTable(val table: TableDescription, val items: List<Item> = emptyList(), val maxPageSize: Int = 1_000) {
    fun retrieve(key: Key) = items.firstOrNull { it.matches(key) }

    fun withItem(item: Item) = retrieve(item.key(table.KeySchema!!))
        .let { existing -> if (existing != null) withoutItem(existing) else this }
        .let { it.copy(items = it.items + item) }

    fun withoutItem(key: Key) = copy(items = items.filterNot { it.matches(key) })

    private fun Item.matches(key: Key) = toList().containsAll(key.toList())
}
