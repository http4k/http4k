package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NO_CONTENT
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SunHttpTest : ServerContract(::SunHttp, ApacheClient()) {

    @Test
    fun `SunHttp does not complain when returning NO_CONTENT`() {
        val err = ByteArrayOutputStream()
        val orig = System.err
        System.setErr(PrintStream(err))

        val app = { _: Request -> Response(NO_CONTENT) }
        val server = app.asServer(SunHttp(8000))
        server.start().use {
            ApacheClient()(Request(GET, "http://localhost:8000/hello"))
            val output = String(err.toByteArray())
            println(output)
            assertThat(output, !containsSubstring("Exception in thread"))
            assertThat(output, !containsSubstring("WARNING: sendResponseHeaders"))
        }

        System.setErr(orig)
    }
}
