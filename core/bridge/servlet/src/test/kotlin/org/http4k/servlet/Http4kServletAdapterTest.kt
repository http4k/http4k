package org.http4k.servlet

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.mock4k.mock
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import javax.servlet.http.HttpServletRequest

class Http4kServletAdapterTest {

    @Test
    fun `bridges request and response`() {
        val response = FakeHttpServletResponse()

        Http4kServletAdapter { Response(OK).body("hello world") }
            .handle(FakeHttpServletRequest(Request(GET, "/")), response)

        assertThat(response.http4k.status, equalTo(OK))
        assertThat(response.http4k.bodyString(), equalTo("hello world"))
    }

    @Test
    fun `returns 501 for unsupported method`() {
        val response = FakeHttpServletResponse()

        Http4kServletAdapter { Response(OK) }
            .handle(object : HttpServletRequest by mock() {
                override fun getMethod() = "PROPFIND"
            }, response)

        assertThat(response.http4k.status, equalTo(NOT_IMPLEMENTED))
    }
}
