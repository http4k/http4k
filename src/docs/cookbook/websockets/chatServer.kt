package cookbook.websockets

import org.http4k.routing.ResourceLoader
import org.http4k.routing.bind
import org.http4k.routing.static
import org.http4k.routing.websockets
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.websocket.WebSocket
import org.http4k.websocket.WsMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

fun main(args: Array<String>) {
    val userCounter = AtomicInteger()
    val messages = mutableListOf<String>()
    val participants = ConcurrentHashMap<String, WebSocket>()

    fun addMessage(new: String) {
        messages.add(new)
        participants.values.forEach { it.send(WsMessage(new)) }
    }

    fun newConnection(ws: WebSocket) {
        val id = "user${userCounter.incrementAndGet()}"
        participants += id to ws
        addMessage("$id joined")
        messages.map { WsMessage(it) }.forEach { ws.send(it) }
        ws.onMessage {
            addMessage("$id: ${it.bodyString()}")
        }
        ws.onClose {
            participants -= id
            addMessage("$id left")
        }
    }

    val app = static(ResourceLoader.Directory("src/test/resources/cookbook/websockets"))
    val websockets = websockets("/ws" bind ::newConnection)

    (app to websockets).asServer(Jetty(9001)).start()
}