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
 * Checks a value type.
 */
fun <T : Value<P>, P : Any> FieldName.check(values: ValueFactory<T, P>, raw: P, message: String) =
    when (val value = if (raw is String) values.parseOrNull(raw) else values.ofOrNull(raw)) {
        null -> Rejected(this, message)
        else -> Valid(value)
    }

/**
 * Checks an enum, matching its constants by name.
 */
inline fun <reified T : Enum<T>> FieldName.check(raw: String, message: String) =
    when (val value = enumValues<T>().firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }) {
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
