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

    /**
     * Helidon fails to provide the headers
     * https://github.com/helidon-io/helidon/issues/9918
     */
    @Disabled
    override fun `can receive headers from upgrade request`() {
        super.`can receive headers from upgrade request`()
    }
}
