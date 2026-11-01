package dev.forkhandles.values

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ConstraintsTest {

    private fun assertProblem(problem: Problem?, constraint: String, message: String) {
        assertThat(problem, present())
        assertThat(problem!!.constraint, equalTo(constraint))
        assertThat(problem.message, equalTo(message))
    }

    @Test
    fun `notNull constraint`() {
        val validator = notNull<Any>()

        assertThat(validator(null), present())
        assertThat(validator("x"), absent())
    }

    @Test
    fun `isNull constraint`() {
        val validator = isNull<Any>()

        assertThat(validator(null), absent())
        assertThat(validator("x"), present())
    }

    @Test
    fun `notBlank constraint`() {
        val validator = notBlank()

        assertThat(validator("hello"), absent())
        assertProblem(validator("   "), "NotBlank", "must not be blank")
        assertProblem(validator(null), "NotBlank", "must not be blank")
    }

    @Test
    fun `notEmpty constraint`() {
        val validator = notEmpty()

        assertThat(validator("hello"), absent())
        assertProblem(validator(""), "NotEmpty", "must not be empty")
    }

    @Test
    fun `size constraint`() {
        val validator = size(min = 2, max = 5)

        assertThat(validator("abc"), absent())
        assertProblem(validator("abcdef"), "Size", "size must be between 2 and 5")
    }

    @Test
    fun `email constraint`() {
        val validator = email()

        assertThat(validator("bob@example.com"), absent())
        assertProblem(validator("not-an-email"), "Email", "must be a well-formed email address")
    }

    @Test
    fun `pattern constraint`() {
        val validator = pattern("\\d+")

        assertThat(validator("123"), absent())
        assertProblem(validator("abc"), "Pattern", "must match \"\\d+\"")
    }

    @Test
    fun `min constraint`() {
        val validator = min(18)

        assertThat(validator(18), absent())
        assertProblem(validator(17), "Min", "must be greater than or equal to 18")
    }

    @Test
    fun `max constraint`() {
        val validator = max(65)

        assertThat(validator(65), absent())
        assertProblem(validator(66), "Max", "must be less than or equal to 65")
    }

    @Test
    fun `decimalMin and decimalMax constraints`() {
        val min = decimalMin(BigDecimal("1.5"))
        val max = decimalMax(BigDecimal("1.5"))

        assertThat(min(BigDecimal("1.5")), absent())
        assertProblem(min(BigDecimal("1.4")), "DecimalMin", "must be greater than or equal to 1.5")

        assertThat(max(BigDecimal("1.5")), absent())
        assertProblem(max(BigDecimal("1.6")), "DecimalMax", "must be less than or equal to 1.5")
    }

    @Test
    fun `positive constraint`() {
        val validator = positive<Int>()

        assertThat(validator(5), absent())
        assertProblem(validator(0), "Positive", "must be greater than 0")
        assertProblem(validator(-5), "Positive", "must be greater than 0")
    }

    @Test
    fun `positiveOrZero constraint`() {
        val validator = positiveOrZero<Int>()

        assertThat(validator(0), absent())
        assertProblem(validator(-1), "PositiveOrZero", "must be greater than or equal to 0")
    }

    @Test
    fun `negative constraint`() {
        val validator = negative<Int>()

        assertThat(validator(-5), absent())
        assertProblem(validator(0), "Negative", "must be less than 0")
        assertProblem(validator(5), "Negative", "must be less than 0")
    }

    @Test
    fun `negativeOrZero constraint`() {
        val validator = negativeOrZero<Int>()

        assertThat(validator(0), absent())
        assertProblem(validator(1), "NegativeOrZero", "must be less than or equal to 0")
    }

    @Test
    fun `digits constraint`() {
        val validator = digits(3, 2)

        assertThat(validator(BigDecimal("123.45")), absent())
        assertProblem(validator(BigDecimal("1234.5")), "Digits", "numeric value out of bounds (<3 digits>.<2 digits> expected)")
        assertProblem(validator(BigDecimal("1.234")), "Digits", "numeric value out of bounds (<3 digits>.<2 digits> expected)")
    }

    @Test
    fun `isTrue constraint`() {
        val validator = isTrue()

        assertThat(validator(true), absent())
        assertProblem(validator(false), "AssertTrue", "must be true")
    }

    @Test
    fun `isFalse constraint`() {
        val validator = isFalse()

        assertThat(validator(false), absent())
        assertProblem(validator(true), "AssertFalse", "must be false")
    }

    @Test
    fun `temporal constraints`() {
        val now = Instant.parse("2020-01-01T00:00:00Z")
        val clock = Clock.fixed(now, ZoneOffset.UTC)

        assertThat(past(clock)(now.minusSeconds(1)), absent())
        assertProblem(past(clock)(now), "Past", "must be a past date")

        assertThat(pastOrPresent(clock)(now), absent())
        assertProblem(pastOrPresent(clock)(now.plusSeconds(1)), "PastOrPresent", "must be a date in the past or in the present")

        assertThat(future(clock)(now.plusSeconds(1)), absent())
        assertProblem(future(clock)(now), "Future", "must be a future date")

        assertThat(futureOrPresent(clock)(now), absent())
        assertProblem(futureOrPresent(clock)(now.minusSeconds(1)), "FutureOrPresent", "must be a date in the present or in the future")
    }

    @Test
    fun `collection constraints`() {
        val notEmpty = notEmptyCollection()
        val sized = sizeCollection(min = 1, max = 2)

        assertThat(notEmpty(listOf("a")), absent())
        assertProblem(notEmpty(emptyList<Any>()), "NotEmpty", "must not be empty")

        assertThat(sized(listOf("a", "b")), absent())
        assertProblem(sized(listOf("a", "b", "c")), "Size", "size must be between 1 and 2")
    }
}
