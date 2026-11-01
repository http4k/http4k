package dev.forkhandles.values

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

private val EMAIL = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")

fun <T> notNull(): Validator<T?, Problem> = { value ->
    if (value == null) ConstraintProblem("NotNull", "must not be null", null) else null
}

fun <T> isNull(): Validator<T?, Problem> = { value ->
    if (value != null) ConstraintProblem("Null", "must be null", value) else null
}

fun notBlank(): Validator<String?, Problem> = { value ->
    if (value.isNullOrBlank()) ConstraintProblem("NotBlank", "must not be blank", value) else null
}

fun notEmpty(): Validator<String?, Problem> = { value ->
    if (value.isNullOrEmpty()) ConstraintProblem("NotEmpty", "must not be empty", value) else null
}

fun size(min: Int = 0, max: Int = Int.MAX_VALUE): Validator<String?, Problem> = { value ->
    if (value == null || value.length !in min..max) {
        ConstraintProblem("Size", "size must be between $min and $max", value)
    } else {
        null
    }
}

fun email(): Validator<String?, Problem> = { value ->
    if (value == null || !EMAIL.matches(value)) {
        ConstraintProblem("Email", "must be a well-formed email address", value)
    } else {
        null
    }
}

fun pattern(regex: String): Validator<String?, Problem> = pattern(regex.toRegex())

fun pattern(regex: Regex): Validator<String?, Problem> = { value ->
    if (value == null || !regex.matches(value)) {
        ConstraintProblem("Pattern", "must match \"$regex\"", value)
    } else {
        null
    }
}

fun <T : Comparable<T>> min(threshold: T): Validator<T?, Problem> = { value ->
    if (value == null || value < threshold) {
        ConstraintProblem("Min", "must be greater than or equal to $threshold", value)
    } else {
        null
    }
}

fun <T : Comparable<T>> max(threshold: T): Validator<T?, Problem> = { value ->
    if (value == null || value > threshold) {
        ConstraintProblem("Max", "must be less than or equal to $threshold", value)
    } else {
        null
    }
}

fun decimalMin(threshold: BigDecimal): Validator<BigDecimal?, Problem> = { value ->
    if (value == null || value < threshold) {
        ConstraintProblem("DecimalMin", "must be greater than or equal to $threshold", value)
    } else {
        null
    }
}

fun decimalMax(threshold: BigDecimal): Validator<BigDecimal?, Problem> = { value ->
    if (value == null || value > threshold) {
        ConstraintProblem("DecimalMax", "must be less than or equal to $threshold", value)
    } else {
        null
    }
}

fun <T : Number> positive(): Validator<T?, Problem> = { value ->
    if (value == null || BigDecimal(value.toString()) <= BigDecimal.ZERO) {
        ConstraintProblem("Positive", "must be greater than 0", value)
    } else {
        null
    }
}

fun <T : Number> positiveOrZero(): Validator<T?, Problem> = { value ->
    if (value == null || BigDecimal(value.toString()) < BigDecimal.ZERO) {
        ConstraintProblem("PositiveOrZero", "must be greater than or equal to 0", value)
    } else {
        null
    }
}

fun <T : Number> negative(): Validator<T?, Problem> = { value ->
    if (value == null || BigDecimal(value.toString()) >= BigDecimal.ZERO) {
        ConstraintProblem("Negative", "must be less than 0", value)
    } else {
        null
    }
}

fun <T : Number> negativeOrZero(): Validator<T?, Problem> = { value ->
    if (value == null || BigDecimal(value.toString()) > BigDecimal.ZERO) {
        ConstraintProblem("NegativeOrZero", "must be less than or equal to 0", value)
    } else {
        null
    }
}

fun digits(integer: Int, fraction: Int): Validator<BigDecimal?, Problem> = { value ->
    val message = "numeric value out of bounds (<$integer digits>.<$fraction digits> expected)"
    when {
        value == null -> ConstraintProblem("Digits", message, value)

        else -> {
            val split = value.stripTrailingZeros().toPlainString().split('.')
            val integerDigits = split[0].removePrefix("-").length
            val fractionDigits = split.getOrElse(1) { "" }.length
            if (integerDigits > integer || fractionDigits > fraction) {
                ConstraintProblem("Digits", message, value)
            } else {
                null
            }
        }
    }
}

fun isTrue(): Validator<Boolean?, Problem> = { value ->
    if (value != true) ConstraintProblem("AssertTrue", "must be true", value) else null
}

fun isFalse(): Validator<Boolean?, Problem> = { value ->
    if (value != false) ConstraintProblem("AssertFalse", "must be false", value) else null
}

fun past(clock: Clock = Clock.systemUTC()): Validator<Instant?, Problem> = { value ->
    if (value == null || !value.isBefore(Instant.now(clock))) {
        ConstraintProblem("Past", "must be a past date", value)
    } else {
        null
    }
}

fun pastOrPresent(clock: Clock = Clock.systemUTC()): Validator<Instant?, Problem> = { value ->
    if (value == null || value.isAfter(Instant.now(clock))) {
        ConstraintProblem("PastOrPresent", "must be a date in the past or in the present", value)
    } else {
        null
    }
}

fun future(clock: Clock = Clock.systemUTC()): Validator<Instant?, Problem> = { value ->
    if (value == null || !value.isAfter(Instant.now(clock))) {
        ConstraintProblem("Future", "must be a future date", value)
    } else {
        null
    }
}

fun futureOrPresent(clock: Clock = Clock.systemUTC()): Validator<Instant?, Problem> = { value ->
    if (value == null || value.isBefore(Instant.now(clock))) {
        ConstraintProblem("FutureOrPresent", "must be a date in the present or in the future", value)
    } else {
        null
    }
}

fun notEmptyCollection(): Validator<Collection<*>?, Problem> = { value ->
    if (value.isNullOrEmpty()) {
        ConstraintProblem("NotEmpty", "must not be empty", value)
    } else {
        null
    }
}

fun sizeCollection(min: Int = 0, max: Int = Int.MAX_VALUE): Validator<Collection<*>?, Problem> = { value ->
    if (value == null || value.size !in min..max) {
        ConstraintProblem("Size", "size must be between $min and $max", value)
    } else {
        null
    }
}
