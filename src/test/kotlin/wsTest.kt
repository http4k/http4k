
import org.http4k.format.Jackson.auto
import org.http4k.websocket.WebSocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.bind
import org.http4k.websocket.websocket

data class SomeBlob(val message: String)

val msg = WsMessage.auto<SomeBlob>().toLens()

fun main(args: Array<String>) {

    val a = "/bob" bind { ws: WebSocket ->
        ws.onMessage {
            println(it)
            ws(WsMessage("hello"))
        }
        ws.onClose { println(it) }
        ws.onError { println(it) }
    }

    val sockets = websocket(a, a)

//    val ab: WsHandler = sockets(Request(Method.GET, "ws://localhost:8000/bob"))!!
//
//    val memoryWebSocket = MemoryWebSocket()
//    ab(memoryWebSocket)
//
//    memoryWebSocket(WsMessage("bob"))

//    sockets(msg(SomeJsonBlob("foo")))
}