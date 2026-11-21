/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import dev.forkhandles.values.LongValue
import dev.forkhandles.values.LongValueFactory
import dev.forkhandles.values.Value
import kotlin.reflect.KProperty1

/**
 * Where the next page starts. Safe to put in a URL.
 */
class Cursor private constructor(value: Long) : LongValue(value) {
    companion object : LongValueFactory<Cursor>(::Cursor)
}

data class Rows<T>(val items: List<T>, val next: Cursor?) : Iterable<T> by items

/**
 * How a page is ordered: Oldest or Newest, which page with a cursor, or by a field, which takes a limit but no cursor.
 */
sealed interface Order<in T> {
    data object Oldest : Order<Any?>
    data object Newest : Order<Any?>
    class By<T> @PublishedApi internal constructor(val field: KProperty1<T, *>, val descending: Boolean, val type: Class<*>, val key: (T) -> Comparable<*>?) : Order<T>
}

inline fun <T, reified V : Comparable<*>> ascending(field: KProperty1<T, V?>): Order<T> = Order.By(field, false, V::class.java, field::get)

inline fun <T, reified V : Comparable<*>> descending(field: KProperty1<T, V?>): Order<T> = Order.By(field, true, V::class.java, field::get)

@JvmName("ascendingValue")
inline fun <T, reified P : Comparable<*>, V : Value<P>> ascending(field: KProperty1<T, V?>): Order<T> = Order.By(field, false, P::class.java) { field.get(it)?.value }

@JvmName("descendingValue")
inline fun <T, reified P : Comparable<*>, V : Value<P>> descending(field: KProperty1<T, V?>): Order<T> = Order.By(field, true, P::class.java) { field.get(it)?.value }

/**
 * A path to a JSON field of a stored document.
 */
class Field<T, V> internal constructor(val path: List<String>, val get: (T) -> V)

/**
 * A field inside a property's value, as in (Booking::id / ShowSeat::show) eq show.
 */
operator fun <T, V : Any, W> KProperty1<T, V>.div(next: KProperty1<V, W>): Field<T, W> = Field(listOf(name, next.name)) { next.get(get(it)) }

private fun <T, V> KProperty1<T, V>.asField() = Field(listOf(name), ::get)

/**
 * One condition of a Where.
 */
sealed interface Clause<T> {
    val field: Field<T, *>
    fun matches(row: T): Boolean
}

/**
 * A field equal to one of some values, or to none of them when negated. Values compare as the JSON they're stored as,
 * so strings, numbers, booleans and composite values all work, as does null.
 */
class Among<T>(override val field: Field<T, *>, val values: List<Any?>, val negated: Boolean) : Clause<T> {
    override fun matches(row: T) = (field.get(row) in values) != negated
}

/**
 * A string field containing some text, ignoring case.
 */
class Containing<T>(override val field: Field<T, out String?>, val text: String) : Clause<T> {
    override fun matches(row: T) = field.get(row)?.contains(text, ignoreCase = true) == true
}

/**
 * A filter on stored documents, built with eq, neq, oneOf and contains, combined with and.
 */
class Where<T> internal constructor(val clauses: List<Clause<T>>) {
    infix fun and(other: Where<T>) = Where(clauses + other.clauses)

    fun matches(row: T) = clauses.all { it.matches(row) }
}

infix fun <T, V> Field<T, V>.eq(value: V) = Where(listOf(Among(this, listOf(value), false)))

infix fun <T, V> Field<T, V>.neq(value: V) = Where(listOf(Among(this, listOf(value), true)))

infix fun <T, V> Field<T, V>.oneOf(values: Collection<V>) = Where(listOf(Among(this, values.toList(), false)))

infix fun <T, V> KProperty1<T, V>.eq(value: V) = asField() eq value

infix fun <T, V> KProperty1<T, V>.neq(value: V) = asField() neq value

infix fun <T, V> KProperty1<T, V>.oneOf(values: Collection<V>) = asField() oneOf values

infix fun <T> Field<T, out String?>.contains(text: String) = Where(listOf(Containing(this, text)))

@JvmName("containsValue")
infix fun <T, V : Value<String>> Field<T, out V?>.contains(text: String) = Where(listOf(Containing(Field<T, String?>(path) { get(it)?.value }, text)))

infix fun <T> KProperty1<T, String?>.contains(text: String) = asField() contains text

@JvmName("containsValueProperty")
infix fun <T, V : Value<String>> KProperty1<T, V?>.contains(text: String) = asField() contains text

/**
 * A filter that matches every row.
 */
fun <T> everything() = Where<T>(emptyList())
