package org.http4k.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.body.Form
import org.http4k.http.core.body.form
import org.http4k.http.core.body.toBody
import org.junit.Test

class FormTest {

    @Test
    fun can_add_to_request_and_extract_it() {
        val form: Form = listOf("a" to "b")
        val get = get("ignored", body = form.toBody())
        val actual = get.form()
        assertThat(actual, equalTo(form))
    }
}