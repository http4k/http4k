package org.http4k.testing

import org.http4k.core.Headers
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.websocket.WebsocketFactory
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsHandler

object TestWebsocketFactory {

    operator fun invoke(server: WsHandler) = object: WebsocketFactory {

        override suspend fun nonBlocking(
            uri: Uri,
            headers: Headers,
            onError: (Throwable) -> Unit,
            onConnect: WsConsumer
        ) = Request(GET, uri)
            .headers(headers)
            .let { server.testWebsocket(it) }
            .also(onConnect)

        override suspend fun blocking(uri: Uri, headers: Headers) = Request(GET, uri)
            .headers(headers)
            .let { server.testWsClient(it) }
    }
}
