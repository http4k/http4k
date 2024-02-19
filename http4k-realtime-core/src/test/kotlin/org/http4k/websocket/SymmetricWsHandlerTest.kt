package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.testing.toSymmetric
import org.junit.jupiter.api.Test

class SymmetricWsHandlerTest {

    private val messages = mutableListOf<String>()
    private val handler = { _: Request ->
        WsResponse { ws ->
            ws.onMessage { messages += it.bodyString() }
        }
    }.toSymmetric()

    @Test
    fun `open websocket directly from handler`() {
        handler(Request(Method.GET, "/"))
            .send(WsMessage("hi"))

        assertThat(messages, equalTo(listOf("hi")))
    }
}
