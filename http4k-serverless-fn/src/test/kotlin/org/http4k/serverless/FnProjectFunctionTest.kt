package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test


class FnProjectFunctionTest {

    @Test
    fun `calls the handler and returns proper body`() {
        val app = { req: Request -> Response(OK).body(req.bodyString()).headers(req.headers) }
        val request = Request(GET, "").body("hello fn")

        val context = FakeHTTPGatewayContext(request)

        val body = object : FnProjectFunction(AppLoader { app }) {}.handleRequest(context, request.bodyString().toByteArray())

        assertThat(context.response.removeHeader("x-http4k-context"), equalTo(Response(OK)))
        assertThat(String(body), equalTo("hello fn"))
    }
}
