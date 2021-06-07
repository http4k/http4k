package org.http4k.strikt

import org.http4k.lens.FormField
import org.http4k.lens.WebForm
import org.http4k.strikt.field
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class FormAssertionsTest {

    @Test
    fun assertions() {
        val form = WebForm(mapOf("foo" to listOf("bar")))

        expectThat(form) {
            field(FormField.required("foo")).isEqualTo("bar")
        }
    }
}
