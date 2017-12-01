package guide.testing

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import cookbook.websockets.WebsocketContract
import org.http4k.client.WebsocketClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.testing.WsClient
import org.http4k.testing.testWsClient
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.junit.After
import org.junit.Before
import org.junit.Test

val namePath = Path.of("name")

// here is our websocket app - it uses dynamic path binding and lenses
val testApp: WsHandler = websockets(
    "/{name}" bind { ws: Websocket ->
        val name = namePath(ws.upgradeRequest)
        ws.send(WsMessage("hello $name"))
    }
)

// this is the abstract contract that defines the behaviour to be tested
abstract class WebsocketContract {
    // subclasses only have to supply a blocking WsClient
    abstract fun client(): WsClient

    @Test
    fun `echoes back connected name`() {
        client().received().take(1).toList() shouldMatch equalTo(listOf(WsMessage("hello bob")))
    }
}

// a unit test version of the contract - it connects to the websocket in memory with no network
class WebsocketUnitTest : WebsocketContract() {
    override fun client() = cookbook.websockets.testApp.testWsClient(Request(GET, "/bob"))!!
}

// a integration test version of the contract - it starts a server and connects to the websocket over the network
class WebsocketServerTest : WebsocketContract() {
    override fun client() = WebsocketClient.blocking(Uri.of("ws://localhost:8000/bob"))

    private val server = cookbook.websockets.testApp.asServer(Jetty(8000))

    @Before
    fun before() {
        server.start()
    }

    @After
    fun after() = server.stop()
}