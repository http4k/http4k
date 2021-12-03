package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
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

        { _: Request -> consumer }.testSseClient(Request(GET, "/"))

        assertThat(consumer.sse.connectRequest, equalTo(Request(GET, "/")))
    }

    @Test
    fun `sends inbound messages to the client`() {
        val client = { _: Request ->
            { ws: Sse ->
                ws.send(message)
                ws.send(message)
                ws.close()
            }
        }.testSseClient(Request(GET, "/"))

        val received = client.received()
        assertThat(received.take(2).toList(), equalTo(listOf(message, message)))
    }

    @Test
    fun `closed sse`() {
        val client = { _: Request ->
            { ws: Sse ->
                ws.close()
            }
        }.testSseClient(Request(GET, "/"))

        assertThat(client.received().none(), equalTo(true))
    }

    @Test
    fun `no match is just cosed`() {
        val actual = object : SseHandler {
            override fun invoke(request: Request): SseConsumer = { it.close() }
        }.testSseClient(Request(GET, "/"))

        assertThat(actual.received().none(), equalTo(true))
    }

    @Test
    fun `when no messages`() {
        val client = { _: Request ->
            { ws: Sse ->
                ws.close()
            }
        }.testSseClient(Request(GET, "/"))

        assertThat(client.received().none(), equalTo(true))
    }
}
