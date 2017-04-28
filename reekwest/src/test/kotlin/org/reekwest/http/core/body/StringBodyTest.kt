package org.reekwest.http.core.body

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.bodyString

class StringBodyTest {
    @Test
    fun can_use_string_as_entity() {
        assertThat(ok().bodyString("abc").bodyString(), equalTo("abc"))
    }
}