package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.sse.SseMessage
import org.http4k.sse.SseResponse
import org.http4k.sse.then
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class SseDebuggingExtensionTests {

    @Test
    fun `SSE - debug request`() {
        val os = ByteArrayOutputStream()
        val req = Request(GET, "").body("anything".byteInputStream())

        val socket = DebuggingFilters.PrintSseRequest(PrintStream(os))
            .then {
                SseResponse { sse ->
                    sse.send(SseMessage.Data("hello"))
                }
            }

        socket.testSseClient(req)

        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring("<<stream>>"))
    }

    @Test
    fun `SSE - debug response`() {
        val os = ByteArrayOutputStream()
        val req = Request(GET, "").body("anything".byteInputStream())

        val socket = DebuggingFilters.PrintSseResponse(PrintStream(os))
            .then {
                SseResponse { sse ->
                    sse.send(SseMessage.Data("hello"))
                    sse.close()
                }
            }

        socket.testSseClient(req)

        val actual = String(os.toByteArray())
        assertThat(actual, containsSubstring("***** Sent: Data: hello"))
        assertThat(actual, containsSubstring("***** CONNECTION CLOSED *****"))
    }
}
