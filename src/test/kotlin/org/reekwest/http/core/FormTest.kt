package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class FormTest {

    @Test
    fun can_add_to_request_and_extract_it() {
        val form: Form = listOf("a" to "b")
        val get = Request.get("ignored", listOf("content-type" to APPLICATION_FORM_URLENCODED), form.toEntity())
        val actual = get.extract(FormEntity)
        assertThat(actual, equalTo(form))
    }

    @Test
    fun does_not_attempt_to_extract_if_content_type_is_invalid() {
        val get = Request.get("ignored", entity = listOf("a" to "b").toEntity())
        val form = get.extract(FormEntity)
        assertThat(form.size, equalTo(0))
    }
}