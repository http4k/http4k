package dev.forkhandles.values

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

private val EMAIL = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

fun <T> notNull(): Validator<T?, Problem> = { value ->
    if (value == null) ConstraintProblem(Constraint.NotNull, Message("must not be null"), value.toString()) else null
}

fun <T> isNull(): Validator<T?, Problem> = { value ->
    if (value != null) ConstraintProblem(Constraint.Null, Message("must be null"), value.toString()) else null
}

fun notBlank(): Validator<String?, Problem> = { value ->
    if (value.isNullOrBlank()) ConstraintProblem(Constraint.NotBlank, Message("must not be blank"), value.toString()) else null
}

fun notEmpty(): Validator<String?, Problem> = { value ->
    if (value.isNullOrEmpty()) ConstraintProblem(Constraint.NotEmpty, Message("must not be empty"), value.toString()) else null
}

fun size(min: Int = 0, max: Int = Int.MAX_VALUE): Validator<String?, Problem> = { value ->
    if (value == null || value.length !in min..max) {
        ConstraintProblem(Constraint.Size, Message("size must be between $min and $max"), value.toString())
    } else {
        null
    }
}

fun email(): Validator<String?, Problem> = { value ->
    if (value == null || !EMAIL.matches(value)) {
        ConstraintProblem(Constraint.Email, Message("must be a well-formed email address"), value.toString())
    } else {
        null
    }
}

fun pattern(regex: String): Validator<String?, Problem> = pattern(regex.toRegex())

fun pattern(regex: Regex): Validator<String?, Problem> = { value ->
    if (value == null || !regex.matches(value)) {
        ConstraintProblem(Constraint.Pattern, Message("must match \"$regex\""), value.toString())
    } else {
        null
    }
}

fun <T : Comparable<T>> min(threshold: T): Validator<T?, Problem> = { value ->
    if (value == null || value < threshold) {
        ConstraintProblem(Constraint.Min, Message("must be greater than or equal to $threshold"), value.toString())
    } else {
        null
    }
}

fun <T : Comparable<T>> max(threshold: T): Validator<T?, Problem> = { value ->
    if (value == null || value > threshold) {
        ConstraintProblem(Constraint.Max, Message("must be less than or equal to $threshold"), value.toString())
    } else {
        null
    }
}

fun decimalMin(threshold: BigDecimal): Validator<BigDecimal?, Problem> = { value ->
    if (value == null || value < threshold) {
        ConstraintProblem(Constraint.DecimalMin, Message("must be greater than or equal to $threshold"), value.toString())
    } else {
        null
    }
}

fun decimalMax(threshold: BigDecimal): Validator<BigDecimal?, Problem> = { value ->
    if (value == null || value > threshold) {
        ConstraintProblem(Constraint.DecimalMax, Message("must be less than or equal to $threshold"), value.toString())
    } else {
        null
    }
}

fun <T : Number> positive(): Validator<T?, Problem> = { value ->
    if (value == null || BigDecimal(value.toString()) <= BigDecimal.ZERO) {
        ConstraintProblem(Constraint.Positive, Message("must be greater than 0"), value.toString())
    } else {
        null
    }
}

fun <T : Number> positiveOrZero(): Validator<T?, Problem> = { value ->
    if (value == null || BigDecimal(value.toString()) < BigDecimal.ZERO) {
        ConstraintProblem(Constraint.PositiveOrZero, Message("must be greater than or equal to 0"), value.toString())
    } else {
        null
    }
}

fun <T : Number> negative(): Validator<T?, Problem> = { value ->
    if (value == null || BigDecimal(value.toString()) >= BigDecimal.ZERO) {
        ConstraintProblem(Constraint.Negative, Message("must be less than 0"), value.toString())
    } else {
        null
    }
}

fun <T : Number> negativeOrZero(): Validator<T?, Problem> = { value ->
    if (value == null || BigDecimal(value.toString()) > BigDecimal.ZERO) {
        ConstraintProblem(Constraint.NegativeOrZero, Message("must be less than or equal to 0"), value.toString())
    } else {
        null
    }
}

fun digits(integer: Int, fraction: Int): Validator<BigDecimal?, Problem> = { value ->
    val message = Message("numeric value out of bounds (<$integer digits>.<$fraction digits> expected)")
    when {
        value == null -> ConstraintProblem(Constraint.Digits, message, value.toString())

        else -> {
            val split = value.stripTrailingZeros().toPlainString().split('.')
            val integerDigits = split[0].removePrefix("-").length
            val fractionDigits = split.getOrElse(1) { "" }.length
            if (integerDigits > integer || fractionDigits > fraction) {
                ConstraintProblem(Constraint.Digits, message, value.toString())
            } else {
                null
            }
        }
    }
}

fun isTrue(): Validator<Boolean?, Problem> = { value ->
    if (value != true) ConstraintProblem(Constraint.AssertTrue, Message("must be true"), value.toString()) else null
}

fun isFalse(): Validator<Boolean?, Problem> = { value ->
    if (value != false) ConstraintProblem(Constraint.AssertFalse, Message("must be false"), value.toString()) else null
}

fun past(clock: Clock = Clock.systemUTC()): Validator<Instant?, Problem> = { value ->
    if (value == null || !value.isBefore(Instant.now(clock))) {
        ConstraintProblem(Constraint.Past, Message("must be a past date"), value.toString())
    } else {
        null
    }
}

fun pastOrPresent(clock: Clock = Clock.systemUTC()): Validator<Instant?, Problem> = { value ->
    if (value == null || value.isAfter(Instant.now(clock))) {
        ConstraintProblem(Constraint.PastOrPresent, Message("must be a date in the past or in the present"), value.toString())
    } else {
        null
    }
}

fun future(clock: Clock = Clock.systemUTC()): Validator<Instant?, Problem> = { value ->
    if (value == null || !value.isAfter(Instant.now(clock))) {
        ConstraintProblem(Constraint.Future, Message("must be a future date"), value.toString())
    } else {
        null
    }
}

fun futureOrPresent(clock: Clock = Clock.systemUTC()): Validator<Instant?, Problem> = { value ->
    if (value == null || value.isBefore(Instant.now(clock))) {
        ConstraintProblem(Constraint.FutureOrPresent, Message("must be a date in the present or in the future"), value.toString())
    } else {
        null
    }
}

fun notEmptyCollection(): Validator<Collection<*>?, Problem> = { value ->
    if (value.isNullOrEmpty()) {
        ConstraintProblem(Constraint.NotEmpty, Message("must not be empty"), value.toString())
    } else {
        null
    }
}

fun sizeCollection(min: Int = 0, max: Int = Int.MAX_VALUE): Validator<Collection<*>?, Problem> = { value ->
    if (value == null || value.size !in min..max) {
        ConstraintProblem(Constraint.Size, Message("size must be between $min and $max"), value.toString())
    } else {
        null
    }
}
