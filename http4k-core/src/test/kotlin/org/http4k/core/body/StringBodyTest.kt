package org.http4k.core.body

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.Test

class StringBodyTest {
    @Test
    fun can_use_string_as_entity() {
        assertThat(Response(Status.OK).body("abc").bodyString(), equalTo("abc"))
    }
}