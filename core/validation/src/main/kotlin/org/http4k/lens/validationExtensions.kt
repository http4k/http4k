package org.http4k.lens

import dev.forkhandles.values.Problem
import dev.forkhandles.values.Validator

fun <IN : Any, OUT> Lens<IN, OUT>.validated(validator: Validator<OUT, Problem>): Lens<IN, OUT> {
    val original = this
    return Lens(original.meta) { target ->
        val value = original(target)
        validator(value)?.let { throw LensFailure(listOf(Invalid(original.meta)), target = it) }
        value
    }
}

fun <OUT> BodyLens<OUT>.validated(validator: Validator<OUT, Problem>): BodyLens<OUT> {
    val original = this
    return BodyLens(original.metas, original.contentType) { target ->
        val value = original(target)
        validator(value)?.let { throw LensFailure(listOf(Invalid(original.metas.first())), target = it) }
        value
    }
}
