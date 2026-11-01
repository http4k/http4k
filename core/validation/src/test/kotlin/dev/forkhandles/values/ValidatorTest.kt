package dev.forkhandles.values

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.junit.jupiter.api.Test

class ValidatorTest {

    @Test
    fun `returns null when valid and a Problem when invalid`() {
        val validator: Validator<Int, Problem> =
            { if (it >= 0) null else ConstraintProblem("Min", "must be greater than or equal to 0", it) }

        assertThat(validator(1), absent())
        assertThat(validator(-1), present())
    }

    @Test
    fun `lifts a boolean Validation into a Validator`() {
        val validator = 1.minLength.toValidator("MinLength", "must be at least 1 character")

        assertThat(validator("a"), absent())

        val problem = validator("")!!
        assertThat(problem.constraint, equalTo("MinLength"))
        assertThat(problem.message, equalTo("must be at least 1 character"))
        assertThat(problem.value, equalTo(""))
    }

    @Test
    fun `and returns the first failure`() {
        val validator = min(18) and max(65)

        assertThat(validator(30), absent())
        assertThat(validator(10)!!.constraint, equalTo("Min"))
        assertThat(validator(80)!!.constraint, equalTo("Max"))
    }

    @Test
    fun `or passes if either validator passes`() {
        val validator = isNull<String>() or notBlank()

        assertThat(validator(null), absent())
        assertThat(validator("ok"), absent())
        assertThat(validator("   "), present())
    }

    @Test
    fun `allOf aggregates all failures into a Problems`() {
        val validator = allOf(size(min = 2), email())

        assertThat(validator("bob@example.com"), absent())

        val problem = validator("x")!!
        assertThat(problem.causes().map { it.constraint }, equalTo(listOf("Size", "Email")))
    }

    @Test
    fun `resultOf returns Success for a valid value and Failure for an invalid one`() {
        val validator = min(18)

        assertThat(validator.resultOf(21), equalTo<Result<Int?, Problem>>(Success(21)))
        assertThat(
            validator.resultOf(10),
            equalTo<Result<Int?, Problem>>(Failure(ConstraintProblem("Min", "must be greater than or equal to 18", 10)))
        )
    }
}
