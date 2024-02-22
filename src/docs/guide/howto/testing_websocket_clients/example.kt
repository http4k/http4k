package guide.howto.testing_websocket_clients

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import org.http4k.client.JavaWsHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.path
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.testing.toSymmetric
import org.http4k.websocket.SetHostFrom
import org.http4k.websocket.SymmetricWsFilter
import org.http4k.websocket.SymmetricWsHandler
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.then
import org.junit.jupiter.api.Test

// The Websocket client we want to test
class GreetingClient(private val wsHandler: SymmetricWsHandler) {

    val messages = mutableListOf<String>()

    fun connect(name: String) {
        wsHandler(Request(Method.GET, "/v1/$name")).onMessage {
            messages += it.bodyString()
        }
    }
}

// Run our client against a real server
fun main() {
    val host = Uri.of(System.getenv("GREETER_HOST"))

    // the JavaWebSocketClient is a SymmetricWsHandler
    val wsHandler: SymmetricWsHandler = SymmetricWsFilter.SetHostFrom(host)
        .then(JavaWsHandler())

    // The SymmetricWsHandler can be injected directly into the GreetingClient under test
    val client = GreetingClient(wsHandler)

    // open a new websocket
    client.connect("http4k")
}

class GreetingClientTest {
    // build a fake server to test GreetingClient against
    private val serverSockets = mutableMapOf<String, Websocket>()
    private val server = websockets(
        "/v1/{name}" bind { req ->
            val name = req.path("name")!!
            WsResponse {
                serverSockets[name] = it
            }
        }
    )

    @Test
    fun `get greeting`() {
        // convert the server to a SymmetricWsHandler to inject it into GreetingClient
        val client = GreetingClient(server.toSymmetric())

        // open a new websocket
        client.connect("http4k")

        // test the client-server interaction
        serverSockets["http4k"]!!.send(WsMessage("Hello http4k"))
        assertThat(client.messages, hasSize(equalTo(1)))
    }
}


