package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class WsRoutingTest {

    val closed = AtomicReference<WsStatus>()

    @Test
    fun `simple find with path matching`() {

        val request = AtomicReference<Request>()

        val ws = websockets(
            "/path1" bind websockets(
                "/{name}" bind { ws: Websocket ->
                    request.set(ws.upgradeRequest)
                }
            ))

        val sentRequestWithNoUriTemplateHeader = Request(Method.GET, "/path1/correct")

        ws(sentRequestWithNoUriTemplateHeader)(newWebSocket(sentRequestWithNoUriTemplateHeader))
        assertThat(request.get().path("name"), equalTo("correct"))
        assertThat(closed.get(), absent())
    }

    @Test
    fun `not found connection is refused`() {
        val websockets = websockets()

        val request = Request(Method.GET, "/path1/index.html")
        websockets(request)(newWebSocket(request))

        assertThat(closed.get(), equalTo(WsStatus.REFUSE))
    }

    private fun newWebSocket(req: Request) = object : Websocket {
        override val upgradeRequest: Request = req

        override fun send(message: WsMessage) {
        }

        override fun close(status: WsStatus) {
            closed.set(status)
        }

        override fun onError(fn: (Throwable) -> Unit) {
        }

        override fun onClose(fn: (WsStatus) -> Unit) {
        }

        override fun onMessage(fn: (WsMessage) -> Unit) {
        }
    }
}
