/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.parseOrNull
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.Validated.Valid
import org.http4k.fanbelt.input.FieldName

sealed interface Validated<out T> {
    data class Valid<T>(val value: T) : Validated<T>
}

fun <T : Value<P>, P : Any> FieldName.check(values: ValueFactory<T, P>, raw: String, message: String) =
    when (val value = values.parseOrNull(raw)) {
        null -> Rejected(this, message)
        else -> Valid(value)
    }

fun <T : Enum<T>> FieldName.check(values: Iterable<T>, raw: String, message: String) =
    when (val value = values.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }) {
        null -> Rejected(this, message)
        else -> Valid(value)
    }

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

inline fun <T> Validated<T>.orElse(rejected: (Rejected) -> Nothing): T = when (this) {
    is Valid -> value
    is Rejected -> rejected(this)
}
