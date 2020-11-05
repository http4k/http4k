package org.http4k.kotest

import io.kotest.matchers.be
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.WebForm
import org.junit.jupiter.api.Test

class FormMatchersTest {
    @Test
    fun formField() =
        FormField.required("name").let {
            assertMatchAndNonMatch(WebForm().with(it of "bob"), haveFormField(it, be("bob")), haveFormField(it, be("bill")))
        }
}
