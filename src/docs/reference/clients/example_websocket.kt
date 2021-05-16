package reference.clients

import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage

fun main() {

    // a standard websocket app
    val server = websockets(
        "/bob" bind { ws: Websocket ->
            ws.send(WsMessage("bob"))
            ws.onMessage {
                println("server received: $it")
                ws.send(it)
            }
        }
    ).asServer(Jetty(8000)).start()

    // blocking client - connection is done on construction
    val blockingClient = WebsocketClient.blocking(Uri.of("ws://localhost:8000/bob"))
    blockingClient.send(WsMessage("server sent on connection"))
    blockingClient.received().take(2).forEach { println("blocking client received: $it") }
    blockingClient.close()

    // non-blocking client - exposes a Websocket interface for attaching listeners,
    // and connection is done on construction, but doesn't block - the (optional) handler
    // passed to the construction is called on connection.
    val nonBlockingClient = WebsocketClient.nonBlocking(Uri.of("ws://localhost:8000/bob")) {
        it.run {
            send(WsMessage("client sent on connection"))
        }
    }

    nonBlockingClient.onMessage {
        println("non-blocking client received:$it")
    }

    nonBlockingClient.onClose {
        println("non-blocking client closing")
    }

    Thread.sleep(100)

    server.stop()
}
