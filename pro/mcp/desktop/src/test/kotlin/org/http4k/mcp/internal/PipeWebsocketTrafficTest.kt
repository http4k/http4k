package org.http4k.mcp.internal

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ReconnectionMode.Disconnect
import org.http4k.core.Uri
import org.http4k.mcp.internal.McpClientSecurity.None
import org.http4k.routing.poly
import org.http4k.routing.websocket.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.sse.SseMessage.Event
import org.http4k.util.PortBasedTest
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.junit.jupiter.api.Test
import java.io.StringWriter
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import kotlin.concurrent.thread

class PipeWebsocketTrafficTest : PortBasedTest {

    @Test
    fun `pipes input and output to correct place`() {
        val inputMessages = listOf("hello", "world")
        val output = StringWriter()

        val expectedList = listOf(
            Event("message", "data1"),
            Event("message", "data2")
        )
        val received = ArrayBlockingQueue<WsMessage>(1000)

        val server = poly(
            "/ws" bind {
                val toSend = expectedList.iterator()

                WsResponse { ws ->
                    ws.send(WsMessage(Event("ping", "").toMessage()))

                    ws.onMessage {
                        if (!it.bodyString().contains("ping")) {
                            received.put(it)
                            ws.send(WsMessage(toSend.next().toMessage()))
                        }
                    }
                }
            }
        ).asServer(Helidon(0)).start()

        thread(isDaemon = true) {
            pipeWebsocketTraffic(
                inputMessages.joinToString("\n").reader(),
                output,
                Uri.of("ws://localhost:${server.port()}/ws"),
                None,
                Disconnect
            )
        }

        assertThat(received.take().bodyString(), equalTo(inputMessages[0]))
        assertThat(received.take().bodyString(), equalTo(inputMessages[1]))

        assertThat(
            output.toString().trimEnd().split("\n"),
            equalTo(listOf("data1", "data2"))
        )
    }
}
