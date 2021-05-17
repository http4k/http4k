package guide.howto.websockets

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.PolyHandler
import org.http4k.server.asServer
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage

fun main() {
    val namePath = Path.of("name")

    val ws = websockets(
        "/{name}" bind { ws: Websocket ->
            val name = namePath(ws.upgradeRequest)
            ws.send(WsMessage("hello $name"))
            ws.onMessage {
                ws.send(WsMessage("$name is responding"))
            }
            ws.onClose { println("$name is closing") }
        }
    )
    val http = { _: Request -> Response(OK).body("hiya world") }

    PolyHandler(http, ws).asServer(Jetty(9000)).start()
}
