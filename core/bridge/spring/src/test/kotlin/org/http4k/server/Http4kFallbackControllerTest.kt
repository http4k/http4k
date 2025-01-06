package org.http4k.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.servlet.FakeHttpServletRequest
import org.http4k.servlet.FakeHttpServletResponse
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

class Http4kFallbackControllerTest : PortBasedTest {

    class TestController : Http4kFallbackController({ Response(OK).body(it.body).headers(it.headers) })

    @Test
    fun `passes requests through and adapts to servlet`() {
        val response = FakeHttpServletResponse()
        val headers = listOf("header" to "value")

        TestController().fallback(FakeHttpServletRequest(
            Request(GET, "").headers(headers).body("helloworld")), response)

        assertThat(
            response.http4k, hasStatus(OK)
            .and(hasBody("helloworld"))
            .and(hasHeader("header", "value"))
        )
    }
}
