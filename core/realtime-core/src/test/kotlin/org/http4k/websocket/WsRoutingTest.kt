package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.routing.orElse
import org.http4k.routing.query
import org.http4k.routing.websocket.bind
import org.http4k.routing.websockets
import org.http4k.testing.testWsClient
import org.junit.jupiter.api.Test

class WsRoutingTest {

    @Test
    fun `uses router for matching`() {
        val app = websockets(
            query("goodbye") bind websockets {
                it.send(WsMessage("query"))
                it.close()
            },
            orElse bind websockets {
                it.send(WsMessage("vanilla"))
                it.close()
            }
        )

        assertThat(
            app.testWsClient(Request(GET, "/").query("goodbye", "bob")).received().toList(), equalTo(
                listOf(WsMessage("query"))
            )
        )
        assertThat(
            app.testWsClient(Request(GET, "/")).received().toList(), equalTo(
                listOf(WsMessage("vanilla"))
            )
        )
    }
}
