package org.http4k.mcp.internal

import org.http4k.client.WebsocketClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.sse.SseMessage
import org.http4k.websocket.WsMessage
import java.io.Reader
import java.io.Writer
import kotlin.concurrent.thread

fun pipeWebsocketTraffic(input: Reader, output: Writer, uri: Uri, security: McpClientSecurity) {

    val response = security.filter.then { Response(OK).headers(it.headers) }(Request(GET, ""))

    val client = WebsocketClient.nonBlocking(uri, response.headers)

    thread {
        input.buffered().lineSequence().forEach {
            client.send(WsMessage(it))
        }
    }

    client.onMessage {
        when (val sseMessage = SseMessage.parse(it.bodyString())) {
            is SseMessage.Event -> {
                when (sseMessage.event) {
                    "message" -> {
                        output.write("${sseMessage.data}\n")
                        output.flush()
                    }

                    "else" -> {}
                }
            }

            else -> {}
        }
    }
}
