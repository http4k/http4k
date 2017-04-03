package org.reekwest.http.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.entity.Form
import org.reekwest.http.core.entity.FormEntity
import org.reekwest.http.core.entity.extract
import org.reekwest.http.core.entity.toEntity

class FormTest {

    @Test
    fun can_add_to_request_and_extract_it() {
        val form: Form = listOf("a" to "b")
        val get = get("ignored", entity = form.toEntity())
        val actual = get.extract(FormEntity)
        assertThat(actual, equalTo(form))
    }
}