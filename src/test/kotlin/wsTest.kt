import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Jackson.auto
import org.http4k.server.websocket
import org.http4k.websocket.MemoryWebSocket
import org.http4k.websocket.WebSocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsRouter

data class SomeBlob(val message: String)

infix fun String.bind(ws: WsHandler): WsRouter = { ws }

fun main(args: Array<String>) {

    val a: WsRouter = "/bob" bind { ws: WebSocket ->
        ws.onMessage {
            println(it)
            ws(WsMessage("hello"))
        }
        ws.onClose { println(it) }
        ws.onError { println(it) }
    }
    val msg = WsMessage.auto<SomeBlob>().toLens()

    val sockets = websocket(a)

    val ab: WsHandler = sockets(Request(Method.GET, "ws://localhost:8000/bob"))!!

    val memoryWebSocket = MemoryWebSocket()
    ab(memoryWebSocket)

    memoryWebSocket(WsMessage("bob"))

//    sockets(msg(SomeJsonBlob("foo")))
}