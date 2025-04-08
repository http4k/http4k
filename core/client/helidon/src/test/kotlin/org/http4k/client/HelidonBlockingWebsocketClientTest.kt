package org.http4k.client

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isA
import org.http4k.server.Jetty
import org.http4k.websocket.BlockingWebsocketClientContract
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
class HelidonBlockingWebsocketClientTest: BlockingWebsocketClientContract(
    serverConfig = Jetty(0),
    websocketFactory = { HelidonWebsocketClient(timeout = it) }
) {
    override fun <T : Throwable> connectErrorMatcher(): Matcher<T> = isA(
        has(Throwable::message, equalTo("Failed to get address for host does-not-exist"))
    )

    override fun <T : Throwable> connectionClosedErrorMatcher(): Matcher<T> = isA(
        has(Throwable::message, equalTo("Attempt to call writer() on a closed connection"))
    )
}
