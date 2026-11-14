package org.http4k.lens

import dev.forkhandles.values.ConstraintProblem
import dev.forkhandles.values.Problem
import dev.forkhandles.values.Validator
import dev.forkhandles.values.causes

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

fun Failure.toProblem(): Problem = ConstraintProblem(type.name, toString(), "")

fun LensFailure.problems(): List<Problem> = when (val target = this.target) {
    is Problem -> target.causes()
    else -> failures.map { it.toProblem() }
}
