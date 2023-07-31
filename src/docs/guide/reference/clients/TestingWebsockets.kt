package guide.reference.clients

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.WebsocketClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.Path
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.testing.testWsClient
import org.http4k.websocket.WsClient
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

val namePath = Path.of("name")

// here is our websocket app - it uses dynamic path binding and lenses
val testApp: WsHandler = websockets(
    "/{name}" bind { req: Request ->
        WsResponse { ws ->
            val name = namePath(req)
            ws.send(WsMessage("hello $name"))
        }
    }
)

// this is the abstract contract that defines the behaviour to be tested
abstract class WebsocketContract {
    // subclasses only have to supply a blocking WsClient
    abstract fun client(): WsClient

    @Test
    fun `echoes back connected name`() {
        assertThat(
            client().received().take(1).toList(),
            equalTo(listOf(WsMessage("hello bob")))
        )
    }
}

// a unit test version of the contract - it connects to the websocket in memory with no network
class WebsocketUnitTest : WebsocketContract() {
    override fun client() =
        testApp.testWsClient(Request(GET, "/bob"))
}

// a integration test version of the contract -
// it starts a server and connects to the websocket over the network
class WebsocketServerTest : WebsocketContract() {
    override fun client() = WebsocketClient.blocking(Uri.of("ws://localhost:8000/bob"))

    private val server = testApp.asServer(Jetty(8000))

    @BeforeEach
    fun before() {
        server.start()
    }

    @AfterEach
    fun after() {
        server.stop()
    }
}
