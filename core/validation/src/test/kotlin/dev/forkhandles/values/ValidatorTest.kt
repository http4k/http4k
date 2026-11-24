package dev.forkhandles.values

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.junit.jupiter.api.Test

class Password(value: String) : StringValue(value, Maskers.hidden())

data class Person(val name: String, val email: String, val age: Int)

class ValidatorTest {

    @Test
    fun `returns null when valid and a Problem when invalid`() {
        val validator: Validator<Int, Problem> =
            { if (it >= 0) null else ConstraintProblem(Constraint.Min, Message("must be greater than or equal to 0"), it.toString()) }

        assertThat(validator(1), absent())
        assertThat(validator(-1), present())
    }

    @Test
    fun `lifts a boolean Validation into a Validator`() {
        val validator = 1.minLength.toValidator(Constraint("MinLength"), Message("must be at least 1 character"))

        assertThat(validator("a"), absent())

        val problem = validator("")!!
        assertThat(problem.constraint, equalTo(Constraint("MinLength")))
        assertThat(problem.message, equalTo(Message("must be at least 1 character")))
        assertThat(problem.value, equalTo(""))
    }

    @Test
    fun `sensitive values are masked in the problem`() {
        val validator = { password: Password -> password.value.length >= 8 }
            .toValidator(Constraint.Size, Message("must be at least 8 characters"))

        val problem = validator(Password("s3cr3t"))!!

        assertThat(problem.value, equalTo("******"))
        assertThat(problem.value, !equalTo("s3cr3t"))
    }

    @Test
    fun `and returns the first failure`() {
        val validator = min(18) and max(65)

        assertThat(validator(30), absent())
        assertThat(validator(10)!!.constraint, equalTo(Constraint.Min))
        assertThat(validator(80)!!.constraint, equalTo(Constraint.Max))
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
        assertThat(problem.causes().map { it.constraint }, equalTo(listOf(Constraint.Size, Constraint.Email)))
    }

    @Test
    fun `composes validators onto object fields`() {
        val validator = allOf(
            Person::name validated notBlank(),
            Person::email validated email(),
            Person::age validated min(18)
        )

        assertThat(validator(Person("Bob", "bob@example.com", 30)), absent())

        val problem = validator(Person("", "x", 17))!!
        assertThat(problem.causes().map { it.constraint }, equalTo(listOf(Constraint.NotBlank, Constraint.Email, Constraint.Min)))
    }

    @Test
    fun `resultOf returns Success for a valid value and Failure for an invalid one`() {
        val validator = min(18)

        assertThat(validator.resultOf(21), equalTo<Result<Int?, Problem>>(Success(21)))
        assertThat(
            validator.resultOf(10),
            equalTo<Result<Int?, Problem>>(Failure(ConstraintProblem(Constraint.Min, Message("must be greater than or equal to 18"), "10")))
        )
    }
}
