package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.websocket.WebSocket
import org.http4k.websocket.WsMessage
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class WsRoutingTest {

    @Test
    fun `simple find with path matching`() {

        val request = AtomicReference<Request>()

        val ws = websockets(
            "/path1" bind websockets(
                "/{name}" bind { ws: WebSocket ->
                    request.set(ws.upgradeRequest)
                }
            ))

        val sentRequestWithNoUriTemplateHeader = Request(GET, "/path1/correct")
        val a = ws(sentRequestWithNoUriTemplateHeader)
        a!!(object : WebSocket {
            override val upgradeRequest: Request = sentRequestWithNoUriTemplateHeader

            override fun send(message: WsMessage) {
            }

            override fun close(status: Status) {
            }

            override fun onError(fn: (Throwable) -> Unit) {
            }

            override fun onClose(fn: (Status) -> Unit) {
            }

            override fun onMessage(fn: (WsMessage) -> Unit) {
            }

        })
        request.get().path("name") shouldMatch equalTo("correct")
    }

    @Test
    fun `not found`() {
        val websockets = websockets()

        websockets(Request(GET, "/path1/index.html")) shouldMatch absent()
    }
}