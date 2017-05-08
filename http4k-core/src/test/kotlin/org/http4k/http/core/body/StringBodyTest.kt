package org.http4k.http.core.body

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.http.core.Response.Companion.ok
import org.junit.Test

class StringBodyTest {
    @Test
    fun can_use_string_as_entity() {
        assertThat(ok().body("abc").bodyString(), equalTo("abc"))
    }
}