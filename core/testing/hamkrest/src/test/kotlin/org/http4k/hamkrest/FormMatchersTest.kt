package org.http4k.hamkrest

import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.WebForm
import org.junit.jupiter.api.Test

class FormMatchersTest {
    @Test
    fun formField() =
        FormField.required("name").let {
            assertMatchAndNonMatch(WebForm().with(it of "bob"), hasFormField(it, containsSubstring("bob")), hasFormField(it, equalTo("bill")))
        }
}
