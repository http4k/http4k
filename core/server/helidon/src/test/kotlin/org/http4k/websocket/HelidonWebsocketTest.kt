package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.server.Helidon
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
class HelidonWebsocketTest : WebsocketServerContract({ Helidon(it, Immediate) }, JavaHttpClient()) {
    @Disabled
    override fun `should propagate close on server stop`() {
        super.`should propagate close on server stop`()
    }

    /*
     * This ensures that Helidon's websocket listeners are unique per connection.
     * If not true, the helidon listener will throw an error
     */
    @Test
    fun `can create multiple simultaneous connections`() {
        val client1 = WebsocketClient.blocking(Uri.of("ws://localhost:$port/queries?query=foo"))
        val client2 = WebsocketClient.blocking(Uri.of("ws://localhost:$port/queries?query=bar"))

        client1.send(WsMessage("hello"))
        client2.send(WsMessage("hello"))

        assertThat(client1.received().take(1).toList(), equalTo(listOf(WsMessage("foo"))))
        assertThat(client2.received().take(1).toList(), equalTo(listOf(WsMessage("bar"))))
    }
}
