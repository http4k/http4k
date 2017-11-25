
import org.http4k.format.Jackson.auto
import org.http4k.websocket.WsMessage
import org.http4k.websocket.websocket

data class SomeJsonBlob(val message: String)

fun main(args: Array<String>) {

    val msg = WsMessage.auto<SomeJsonBlob>().toLens()

    val websocket1 = websocket {
        onMessage = {
            println(msg(it))
        }
    }
    websocket1(msg(SomeJsonBlob("foo")))
}