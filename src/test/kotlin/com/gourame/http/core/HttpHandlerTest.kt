package com.gourame.http.core

import com.gourame.http.core.Request.Companion.get
import com.gourame.http.core.Status.Companion.OK
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

