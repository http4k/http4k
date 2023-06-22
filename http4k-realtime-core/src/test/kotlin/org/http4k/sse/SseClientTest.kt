package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class SseClientTest {

    private val message = SseMessage.Data("hello")

    private class TestConsumer : SseConsumer {
        lateinit var sse: Sse
        val closed = AtomicReference<Boolean>()

        override fun invoke(p1: Sse) {
            sse = p1
            p1.onClose {
                closed.set(true)
            }
        }
    }

    @Test
    fun `when match, passes a consumer with the matching request`() {
        val consumer = TestConsumer();

        { req: Request ->
            assertThat(req, equalTo(Request(GET, "/")))
            SseResponse(consumer)
        }.testSseClient(Request(GET, "/"))
    }

    @Test
    fun `when match passes HTTP headers back`() {
        val consumer = TestConsumer();

        val client = { req: Request ->
            assertThat(req, equalTo(Request(GET, "/")))
            SseResponse(OK, listOf("foo" to "bar"), consumer)
        }.testSseClient(Request(GET, "/"))

        assertThat(client.status, equalTo(OK))
        assertThat(client.headers, equalTo(listOf("foo" to "bar")))
    }

    @Test
    fun `sends inbound messages to the client`() {
        val client = { _: Request ->
            SseResponse { sse: Sse ->
                sse.send(message)
                sse.send(message)
                sse.close()
            }
        }.testSseClient(Request(GET, "/"))

        val received = client.received()
        assertThat(received.take(2).toList(), equalTo(listOf(message, message)))
    }

    @Test
    fun `closed sse`() {
        val client = { _: Request ->
            SseResponse { sse: Sse ->
                sse.close()
            }
        }.testSseClient(Request(GET, "/"))

        assertThat(client.received().none(), equalTo(true))
    }

    @Test
    fun `no match is just closed`() {
        val actual = object : SseHandler {
            override fun invoke(request: Request) = SseResponse { it.close() }
        }.testSseClient(Request(GET, "/"))

        assertThat(actual.received().none(), equalTo(true))
    }

    @Test
    fun `when no messages`() {
        val client = { _: Request ->
            SseResponse { sse: Sse ->
                sse.close()
            }
        }.testSseClient(Request(GET, "/"))

        assertThat(client.received().none(), equalTo(true))
    }
}
