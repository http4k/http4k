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
import org.http4k.fanbelt.input.FieldName

/**
 * A checked input: Valid, or Rejected with errors per field.
 */
sealed interface Validated<out T>

data class Valid<T>(val value: T) : Validated<T>

/**
 * Checks a raw value as a value type, rejecting this field with the message if it isn't valid. Strings go through the
 * type's parse function.
 */
fun <T : Value<P>, P : Any> FieldName.check(values: ValueFactory<T, P>, raw: P, message: String) =
    when (val value = if (raw is String) values.parseOrNull(raw) else values.ofOrNull(raw)) {
        null -> Rejected(this, message)
        else -> Valid(value)
    }

/**
 * Matches text to an enum constant by name, ignoring case and surrounding whitespace, or rejects this field with the
 * message.
 */
inline fun <reified T : Enum<T>> FieldName.check(raw: String, message: String) =
    when (val value = enumValues<T>().firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }) {
        null -> Rejected(this, message)
        else -> Valid(value)
    }

/**
 * Combines two checks, keeping the errors of both.
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
 * The valid value, or the Rejected passed to a block that must return, as in .orElse { return@operation it }.
 */
inline fun <T> Validated<T>.orElse(rejected: (Rejected) -> Nothing): T = when (this) {
    is Valid -> value
    is Rejected -> rejected(this)
}
