package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.body.Form
import org.reekwest.http.core.body.form
import org.reekwest.http.core.body.toBody

class FormTest {

    @Test
    fun can_add_to_request_and_extract_it() {
        val form: Form = listOf("a" to "b")
        val get = get("ignored", body = form.toBody())
        val actual = get.form()
        assertThat(actual, equalTo(form))
    }
}