package org.http4k.powerassert

import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.WebForm
import org.junit.jupiter.api.Test

class FormMatchersTest {
    @Test
    fun formField() {
        val nameField = FormField.required("name")
        val form = WebForm().with(nameField of "bob")
        
        assert(form.hasFormField(nameField, "bob"))
        assert(!form.hasFormField(nameField, "bill"))
    }
}