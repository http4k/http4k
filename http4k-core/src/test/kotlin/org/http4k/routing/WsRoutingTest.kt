package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class WsRoutingTest {

    @Test
    fun `simple find with path matching`() {

        val request = AtomicReference<Request>()

        val ws = websockets(
            "/path1" bind websockets(
                "/{name}" bind { ws: Websocket ->
                    request.set(ws.upgradeRequest)
                }
            ))

        val sentRequestWithNoUriTemplateHeader = Request(GET, "/path1/correct")
        val a = ws(sentRequestWithNoUriTemplateHeader)
        a!!(object : Websocket {
            override val upgradeRequest: Request = sentRequestWithNoUriTemplateHeader

            override fun send(message: WsMessage) {
            }

            override fun close(status: WsStatus) {
            }

            override fun onError(fn: (Throwable) -> Unit) {
            }

            override fun onClose(fn: (WsStatus) -> Unit) {
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