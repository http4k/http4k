package cookbook.websockets

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.routing.static
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.websocket.WebSocket
import org.http4k.websocket.WsMessage


fun main(args: Array<String>) {

    val app = routes("/ping" bind { _: Request -> Response(OK) },
        static(ResourceLoader.Classpath("/cookbook/websockets"))
    )

    val messages = mutableListOf<String>()
    val participants = mutableSetOf<WebSocket>()

    fun addMessage(wsMessage: WsMessage) {
        messages.add(wsMessage.bodyString())
    }

    fun newConnection(ws: WebSocket) {
        addMessage(WsMessage("joined"))
        participants.add(ws)
        messages.map { WsMessage(it) }.forEach { ws.send(it) }
        ws.onMessage(::addMessage)
        ws.onClose {
            participants.remove(ws)
            addMessage(WsMessage("left"))
        }
    }

    val websockets = websockets("/chat" bind ::newConnection)

    (app to websockets).asServer(Jetty(9001)).start()
}