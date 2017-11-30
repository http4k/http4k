package blog.typesafe_websockets

import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.websocket.WebSocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage

fun main(args: Array<String>) {
    data class Wrapper(val value: String)

    val body = WsMessage.string().map(::Wrapper, Wrapper::value).toLens()

    val nameLens = Path.of("name")

    val ws: WsHandler = websockets(
        "/hello" bind websockets(
            "/{name}" bind { ws: WebSocket ->
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

    ws.asServer(Jetty(9000)).start().block()
}