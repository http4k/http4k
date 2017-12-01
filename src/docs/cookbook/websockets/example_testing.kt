package cookbook.websockets

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.testing.testWsClient
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.junit.Test

val namePath = Path.of("name")

val testApp = websockets(
    "/{name}" bind { ws: Websocket ->
        val name = namePath(ws.upgradeRequest)
        ws.send(WsMessage("hello $name"))
    }
)

class WebsocketAppTest {
    @Test
    fun `echoes back connected name`() {
        val client = testApp.testWsClient(Request(GET, "/bob"))!!
        client.received().take(1).toList() shouldMatch equalTo(listOf(WsMessage("hello bob")))
    }
}