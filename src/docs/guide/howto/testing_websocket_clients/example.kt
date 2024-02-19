package guide.howto.testing_websocket_clients

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaWebSocketClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.path
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
import org.http4k.testing.toSymmetric
import org.http4k.websocket.SymmetricWsFilters
import org.http4k.websocket.SymmetricWsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.then
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

// The Websocket client we want to test
class GreetingClient(private val wsHandler: SymmetricWsHandler) {

    fun getGreeting(name: String): String? {
        val waiter = CountDownLatch(1)
        var greeting: String? = null

        wsHandler(Request(Method.GET, "/v1/$name")).onMessage {
            greeting = it.bodyString()
            waiter.countDown()
        }

        waiter.await()
        return greeting
    }
}

// Run our client against a real server
fun main() {
    val host = Uri.of(System.getenv("GREETER_HOST"))

    // the JavaWebSocketClient is a SymmetricWsHandler
    val wsHandler: SymmetricWsHandler = SymmetricWsFilters.SetHostFrom(host)
        .then(JavaWebSocketClient())

    // The SymmetricWsHandler can be injected directly into the GreetingClient under test
    val client = GreetingClient(wsHandler)

    println(client.getGreeting("http4k"))
}

class GreetingClientTest {
    // build a fake server to test GreetingClient against
    private val server = websockets(
        "/v1/{name}" bind { req ->
            WsResponse {
                it.send(WsMessage("Hello ${req.path("name")}"))
            }
        }
    )

    @Test
    fun foo() {
        // convert the server to a SymmetricWsHandler to inject it into GreetingClient
        val client = GreetingClient(server.toSymmetric())

        // test the client-server interaction
        assertThat(client.getGreeting("http4k"), equalTo("Hello http4k"))
    }
}


