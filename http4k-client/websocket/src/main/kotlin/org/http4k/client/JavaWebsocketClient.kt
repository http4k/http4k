package org.http4k.client

import org.http4k.core.Request
import org.http4k.websocket.Websocket
import org.http4k.websocket.SymmetricWsHandler
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.exceptions.WebsocketNotConnectedException
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class JavaWebsocketClient(
    private val draft: Draft = Draft_6455(),
    private val timeout: Duration? = null,
    private val errorHandler: (Request, Throwable) -> Unit = { _, e -> throw e }
): SymmetricWsHandler {

    override fun invoke(request: Request): Websocket {
        var websocket: Websocket? = null
        val waitForWs = CountDownLatch(1)

        WebsocketClient.nonBlocking(request.uri, request.headers, timeout ?: Duration.ZERO, draft = draft,
            onConnect = {
                websocket = it
                waitForWs.countDown()
            },
            onError = { errorHandler(request, it) }
        )

        if (timeout == null) {
            waitForWs.await()
        } else {
            waitForWs.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
        }

        return websocket ?: throw WebsocketNotConnectedException()
    }
}
