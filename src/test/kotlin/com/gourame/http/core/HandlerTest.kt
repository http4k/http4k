package com.gourame.http.core

import com.gourame.http.core.Method.GET
import com.gourame.http.core.Request
import com.gourame.http.core.Response
import com.gourame.http.core.Status.Companion.OK
import com.gourame.http.core.Uri
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class HandlerTest {
    @Test
    fun basic_handler() {
        val handler = { request: Request -> Response(OK) }
        val response = handler(Request(method = GET, uri = Uri.uri("irrelevant")))
        assertThat(response, equalTo(Response(OK)))
    }
}

