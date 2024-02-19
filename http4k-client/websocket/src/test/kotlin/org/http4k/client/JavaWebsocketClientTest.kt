package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.websocket.SymmetricWsFilters
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.then
import org.junit.jupiter.api.Test

class JavaWebsocketClientTest {

    private val messages = mutableListOf<String>()
    private val wsHandler = { _: Request ->
        WsResponse { ws ->
            ws.onMessage { messages += it.bodyString() }
        }
    }

    private val server = wsHandler.asServer(Jetty(0)).start()

    private val client = SymmetricWsFilters.SetHostFrom(Uri.of("ws://localhost:${server.port()}"))
        .then(JavaWebsocketClient())

    @Test
    fun `open websocket through client`() {
        client(Request(Method.GET, "/"))
            .send(WsMessage("hi"))

        assertThat(messages, equalTo(listOf("hi")))
    }
}
