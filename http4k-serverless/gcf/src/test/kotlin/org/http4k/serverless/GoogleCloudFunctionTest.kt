package org.http4k.serverless

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class GoogleCloudFunctionTest {
    @Test
    fun `calls the handler and returns proper body`() {
        val app = { req: Request -> Response(OK).body(req.bodyString()) }
        val request = FakeGCFRequest(Request(GET, "").body("hello gcf"))
        val response = FakeGCFResponse()

        object : GoogleCloudFunction(AppLoader { app }) {}.service(request, response)

        assertThat((String(response.outputStream.toByteArray())), equalTo("hello gcf"))
    }
}
