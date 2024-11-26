package org.http4k.websocket

import org.http4k.client.JavaHttpClient
import org.http4k.server.Helidon
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.junit.jupiter.api.Disabled

class HelidonWebsocketTest : WebsocketServerContract({ Helidon(it, Immediate) }, JavaHttpClient()) {
    @Disabled
    override fun `should propagate close on server stop`() {
        super.`should propagate close on server stop`()
    }
}
