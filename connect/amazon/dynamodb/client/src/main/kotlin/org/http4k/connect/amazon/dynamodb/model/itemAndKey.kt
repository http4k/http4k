package org.http4k.connect.amazon.dynamodb.model

typealias Item = Map<AttributeName, AttributeValue>
typealias Key = Item

/**
 * Convenience Method to create an Item from a set of Bindings.
 */
fun Item(vararg modifiers: (Item) -> Item): Item =
    mapOf<AttributeName, AttributeValue>().with(*modifiers)

/**
 * Convenience Method to create an Item Key from a set of Bindings.
 */
fun Key(vararg modifiers: (Item) -> Item): Item =
    mapOf<AttributeName, AttributeValue>().with(*modifiers)

fun Item.with(vararg modifiers: (Item) -> Item): Item =
    modifiers.fold(this) { memo, next -> next(memo) }

fun Item.without(vararg attributes: Attribute<*>): Item = minus(attributes.map { it.name })
