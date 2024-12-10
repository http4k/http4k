package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

interface HttpClientWithMemoryModeContract : PortBasedTest {
    val client: HttpHandler
    val port: Int

    @Test
    fun `honors the memory body mode when a request body is an input stream`() {
        val expectedBody = "hello world"

        val request = Request(POST, "http://localhost:$port/echo").body(expectedBody.byteInputStream())

        val response = client(request)

        assertThat(request.bodyString(), equalTo(expectedBody))
        assertThat(response.bodyString(), equalTo(expectedBody))
    }
}
