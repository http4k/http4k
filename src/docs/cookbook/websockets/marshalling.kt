package cookbook.websockets

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.static
import org.http4k.routing.websockets
import org.http4k.testing.testWsClient
import org.http4k.websocket.PolyHandler
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage

data class Wrapper(val value: String)

val body = WsMessage.string().map(::Wrapper, Wrapper::value).toLens()

val nameLens = Path.of("name")

val ws: WsHandler = websockets(
    "/hello" bind websockets(
        "/{name}" bind { ws: Websocket ->
            val name = nameLens(ws.upgradeRequest)
            ws.send(WsMessage("hello $name"))
            ws.onMessage {
                val received = body(it)
                ws.send(body(received))
            }
            ws.onClose {
                println("closed")
            }
        }
    )
)
val app = PolyHandler(static(), ws)

fun main(args: Array<String>) {
    val client = app.testWsClient(Request(Method.GET, "ws://localhost:9000/hello/bob"))!!

    client.send(WsMessage("1"))
    client.close(Status(200, "bob"))

    client.received.take(2).forEach(::println)
}