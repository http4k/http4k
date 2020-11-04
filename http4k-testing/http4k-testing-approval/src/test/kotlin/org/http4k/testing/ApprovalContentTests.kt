package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.ApprovalContent.Companion.EntireHttpMessage
import org.http4k.testing.ApprovalContent.Companion.HttpBodyOnly
import org.junit.jupiter.api.Test

class ApprovalContentTests {

    private val input = Response(OK)
        .header("some-header", "some header value")
        .body("hello")

    @Test
    fun `body only`() {
        assertThat(HttpBodyOnly()(input).reader().use { it.readText() }, equalTo("hello"))
    }

    @Test
    fun `body only with formatter`() {
        assertThat(HttpBodyOnly { it.reversed() }(input).reader().use { it.readText() }, equalTo("olleh"))
    }

    @Test
    fun `entire message`() {
        assertThat(EntireHttpMessage()(input).reader().use { it.readText() }, equalTo(("HTTP/1.1 200 OK\r\n" +
            "some-header: some header value\r\n" +
            "\r\n" +
            "hello")))
    }
}