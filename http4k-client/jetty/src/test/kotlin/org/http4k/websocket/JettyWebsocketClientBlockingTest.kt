package org.http4k.websocket

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import org.eclipse.jetty.websocket.api.exceptions.WebSocketException
import org.http4k.client.JettyWebsocketClient
import org.http4k.server.Jetty
import java.net.UnknownHostException

class JettyWebsocketClientBlockingTest : BlockingWebsocketClientContract(
    serverConfig = Jetty(0),
    websocketFactory = { uri, headers ->
        JettyWebsocketClient.blockingWebsocket(uri, headers)
    }
) {
    override fun <T: Throwable> connectErrorMatcher(): Matcher<T> = isA<UnknownHostException>()
    override fun <T: Throwable> connectionClosedErrorMatcher(): Matcher<T> = isA(
        has(WebSocketException::message, equalTo("not connected"))
    )
}
