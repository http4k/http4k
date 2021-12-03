package blog.nanoservices

import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import java.time.Instant

fun `ticking websocket clock`() =
    { ws: Websocket ->
        while (true) {
            ws.send(WsMessage(Instant.now().toString()))
            Thread.sleep(1000)
        }
    }.asServer(Netty()).start()

fun main() {
    `ticking websocket clock`()
    WebsocketClient.nonBlocking(Uri.of("http://localhost:8000")).onMessage { println(it) }
}
