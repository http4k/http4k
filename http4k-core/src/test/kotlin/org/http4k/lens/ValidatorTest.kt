package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.lens.Validator.Feedback
import org.http4k.lens.Validator.Ignore
import org.http4k.lens.Validator.Strict
import org.junit.jupiter.api.Test

class ValidatorTest {
    private val formFields = FormField.required("bob")

    @Test
    fun strict() {
        assertThat({ Strict(WebForm(), listOf(formFields)) }, throws<LensFailure>())
    }

    @Test
    fun feedback() {
        assertThat(Feedback(WebForm(), listOf(formFields)), equalTo(listOf<Failure>(Missing(formFields.meta))))
    }

    @Test
    fun ignore() {
        assertThat(Ignore(WebForm(), listOf(formFields)), equalTo(emptyList()))
    }
}
