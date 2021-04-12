package blog.nanoservices

import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import java.lang.Runtime.getRuntime
import java.util.Scanner

fun `websocket terminal`() =
    { ws: Websocket ->
        ws.onMessage {
            ws.send(WsMessage(
                getRuntime()
                    .exec(it.bodyString())
                    .inputStream.reader().readText()
            ))
        }
    }.asServer(Netty()).start()

fun main() {
    `websocket terminal`()

    val ws = WebsocketClient.nonBlocking(Uri.of("http://localhost:8000"))
    ws.onMessage { println(it.bodyString()) }

    val scan = Scanner(System.`in`)
    while (true) {
        ws.send(WsMessage(scan.nextLine()))
    }
}
