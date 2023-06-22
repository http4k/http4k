package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Methodimport org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.routing.ws.bind
import org.http4k.websocket.WsResponse
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
                "/{name}" bind { req ->
                    WsResponse { ws -> request.set(req) }
                }
            ))

        val sentRequestWithNoUriTemplateHeader = Request(GET, "/path1/correct")

        ws(sentRequestWithNoUriTemplateHeader)
        assertThat(request.get().path("name"), equalTo("correct"))
        assertThat(closed.get(), absent())
    }

    @Test
    fun `not found connection is refused`() {
        val websockets = websockets()

        val request = Request(GET, "/path1/index.html")
        websockets(request)

        assertThat(closed.get(), equalTo(WsStatus.REFUSE))
    }
}
