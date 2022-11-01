package org.http4k.client

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import org.http4k.server.Undertow
import org.http4k.websocket.BlockingWebsocketClientContract
import java.net.UnknownHostException

class OkHttpWebsocketClientBlockingTest : BlockingWebsocketClientContract(
    serverConfig = Undertow(0),
    websocketFactory = { uri, headers, timeout ->
        OkHttpWebsocketClient.blocking(uri, headers, timeout)
    }
) {
    override fun <T : Throwable> connectErrorMatcher(): Matcher<T> = isA<UnknownHostException>()

    override fun <T : Throwable> connectionClosedErrorMatcher(): Matcher<T> = isA(
        has(IllegalStateException::message, equalTo("Connection to ws://localhost:$port/bob is closed."))
    )
}
