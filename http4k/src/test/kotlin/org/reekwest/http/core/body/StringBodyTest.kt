package org.http4k.http.core.body

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.http4k.http.core.Response.Companion.ok

class StringBodyTest {
    @Test
    fun can_use_string_as_entity() {
        assertThat(ok().body("abc").bodyString(), equalTo("abc"))
    }
}