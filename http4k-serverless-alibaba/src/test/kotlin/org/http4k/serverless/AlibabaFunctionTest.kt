package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.servlet.FakeHttpServletRequest
import org.http4k.servlet.FakeHttpServletResponse
import org.junit.jupiter.api.Test


class AlibabaFunctionTest {
    @Test
    fun `calls the handler and returns proper body`() {
        val app = { req: Request -> Response(OK).body(req.bodyString()) }
        val request = FakeHttpServletRequest(Request(GET, "").body("hello alibaba"))
        val response = FakeHttpServletResponse()

        object : AlibabaFunction(AppLoader { app }) {}.handleRequest(request, response, null)

        assertThat(response.http4k.bodyString(), equalTo("hello alibaba"))
    }
}
