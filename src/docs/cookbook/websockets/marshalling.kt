package cookbook.websockets

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.routing.RoutingWsHandler
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.websocket.WebSocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.asClient

data class MyIntWrapper(val v: Int)

val body = WsMessage.string().map({ MyIntWrapper(it.toInt()) }, { it.v.toString() }).toLens()

private val ws: RoutingWsHandler = websockets(
    "/hello" bind websockets(
        "/{name}" bind { ws: WebSocket ->
            val name = Path.of("name")(ws.upgradeRequest)
            println("hello " + name)
            ws.onMessage {
                val received = body(it)
                println("$name got " + received)
                ws.send(body(MyIntWrapper(123 * received.v)))
            }
            ws.onClose {
                println("closed")
            }
        }
    )
)

fun main(args: Array<String>) {

    val client = ws.asClient(Request(Method.GET, "/hello/same"))!!
    client.send(WsMessage("1"))
    client.send(WsMessage("2"))
    client.close(Status(200, "bob"))

    client.received.take(3).forEach {
        println("received back: " + body(it))
    }
}