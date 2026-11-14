/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.ofOrNull
import dev.forkhandles.values.parseOrNull
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.Validated.Valid
import org.http4k.fanbelt.input.FieldName

/**
 * Input checked against a value type: `Valid` with the value, or a `Rejected` with the errors per field, which an
 * operation can return as it is.
 */
sealed interface Validated<out T> {
    data class Valid<T>(val value: T) : Validated<T>
}

/**
 * Checks raw input as the value type, or rejects this field with the message. Text goes through the value type's own
 * parsing (trimming, say); a value that arrived typed is checked as it is.
 */
fun <T : Value<P>, P : Any> FieldName.check(values: ValueFactory<T, P>, raw: P, message: String) =
    when (val value = if (raw is String) values.parseOrNull(raw) else values.ofOrNull(raw)) {
        null -> Rejected(this, message)
        else -> Valid(value)
    }

/**
 * Matches raw text to one of the entries by name, ignoring case and surrounding space, or rejects this field with the
 * message.
 */
fun <T : Enum<T>> FieldName.check(values: Iterable<T>, raw: String, message: String) =
    when (val value = values.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }) {
        null -> Rejected(this, message)
        else -> Valid(value)
    }

/**
 * Combines two checks into one value, keeping the errors of both when either is rejected.
 */
fun <A, B, C> Validated<A>.zip(other: Validated<B>, combine: (A, B) -> C): Validated<C> = when (this) {
    is Valid -> when (other) {
        is Valid -> Valid(combine(value, other.value))
        is Rejected -> other
    }

    is Rejected -> when (other) {
        is Valid -> this
        is Rejected -> Rejected(errors + other.errors)
    }
}

/**
 * The checked value, or the `Rejected` handed to a block that must leave, as in `.orElse { return@operation it }`.
 */
inline fun <T> Validated<T>.orElse(rejected: (Rejected) -> Nothing): T = when (this) {
    is Valid -> value
    is Rejected -> rejected(this)
}
