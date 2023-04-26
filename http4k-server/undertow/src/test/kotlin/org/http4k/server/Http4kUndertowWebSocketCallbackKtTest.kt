package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.undertow.server.HttpServerExchange
import io.undertow.util.HeaderMap
import io.undertow.util.HttpString
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class Http4kUndertowWebSocketCallbackKtTest {

    @ParameterizedTest
    @ValueSource(strings = ["upgrade", "Upgrade", "keep-alive, Upgrade"])
    fun `upgrades when valid Connection header provided`(connectionHeaderValue: String) {
        val predicate = requiresWebSocketUpgrade()
        val headerMap = HeaderMap().apply {
            this.add(HttpString("Connection"), connectionHeaderValue)
            this.add(HttpString("Upgrade"), "websocket")
        }
        val httpServerExchange = HttpServerExchange(null, headerMap, null, 0)

        val requiresWebsocketUpgrade = predicate(httpServerExchange)

        assertThat(requiresWebsocketUpgrade,  equalTo(true))
    }
}
