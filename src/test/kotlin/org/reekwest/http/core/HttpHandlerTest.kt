package org.reekwest.http.core

import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Status.Companion.OK
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class HttpHandlerTest {
    @Test
    fun basic_handler() {
        val handler = { _: Request -> Response(OK) }
        val response = handler(get("irrelevant"))
        assertThat(response, equalTo(Response(OK)))
    }
}

