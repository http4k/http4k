package dev.forkhandles.values

typealias Validator<T, P> = (T) -> P?

fun <T> Validation<T>.toValidator(constraint: String, message: String): Validator<T, Problem> = { value ->
    if (this(value)) null else ConstraintProblem(constraint, message, value)
}

infix fun <T> Validator<T, Problem>.and(other: Validator<T, Problem>): Validator<T, Problem> = { value ->
    this(value) ?: other(value)
}

infix fun <T> Validator<T, Problem>.or(other: Validator<T, Problem>): Validator<T, Problem> = { value ->
    val left = this(value)
    val right = other(value)
    when {
        left == null || right == null -> null
        else -> Problems(listOf(left, right))
    }
}

fun <T> allOf(vararg validators: Validator<T, Problem>): Validator<T, Problem> = { value ->
    validators.mapNotNull { it(value) }.flatMap { it.causes() }.let { causes ->
        if (causes.isEmpty()) null else Problems(causes)
    }
}
