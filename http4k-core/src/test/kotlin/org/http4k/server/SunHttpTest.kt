package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NO_CONTENT
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SunHttpTest : ServerContract(::SunHttp, ApacheClient()) {

    @Test
    fun `does not complain when returning NO_CONTENT`() {
        checkOutput(
            !containsSubstring("Exception thread")
                .and(containsSubstring("WARNING: sendResponseHeaders"))
        ) { Response(NO_CONTENT) }
    }

    @Test
    fun `does not complain when body not consumed`() {
        checkOutput(!containsSubstring("Exception"), { it.body("hello".byteInputStream()) }) { Response(NO_CONTENT) }
    }

    private fun checkOutput(
        expected: Matcher<CharSequence>,
        modifyRequest: (Request) -> Request = { it },
        app: HttpHandler
    ) {
        val orig = System.err
        try {
            val err = ByteArrayOutputStream()
            System.setErr(PrintStream(err))

            val server = app.asServer(SunHttp(0))
            server.start().use {
                ApacheClient()(modifyRequest(Request(POST, "http://localhost:${it.port()}/hello")))
                assertThat(String(err.toByteArray()).also(::println), expected)
            }
        } finally {
            System.setErr(orig)
        }
    }
}
