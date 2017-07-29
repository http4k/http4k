package org.http4k.hamkrest

import com.natpryce.hamkrest.equalTo
import org.http4k.lens.FormField
import org.http4k.lens.WebForm
import org.junit.Test

class FormMatchersTest {
    @Test
    fun `formField`() =
        FormField.required("name").let {
            assertMatchAndNonMatch(WebForm().with(it of "bob"), hasFormField(it, equalTo("bob")), hasFormField(it, equalTo("bill")))
        }
}