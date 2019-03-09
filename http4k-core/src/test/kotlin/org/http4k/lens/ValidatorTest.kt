package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.lens.Validator.Feedback
import org.http4k.lens.Validator.Ignore
import org.http4k.lens.Validator.None
import org.http4k.lens.Validator.Strict
import org.junit.jupiter.api.Test

class ValidatorTest {
    private val formFields = FormField.required("bob")

    @Test
    fun strict() {
        assertThat({
            Strict(WebForm(), formFields)
        }, throws<LensFailure>())
    }

    @Test
    fun feedback() {
        assertThat(Feedback(WebForm(), formFields), equalTo(listOf<Failure>(Missing(formFields.meta))))
    }

    @Test
    fun ignore() {
        assertThat(Ignore(WebForm(), formFields), equalTo(emptyList()))
    }

    @Test
    fun none() {
        var called = false
        val formFields = FormField.map {
            called = true
            it
        }.required("bob")

        assertThat(None(WebForm(mapOf("bob" to listOf("asd"))), formFields), equalTo(emptyList()))
        assertThat(called, equalTo(false))
    }
}