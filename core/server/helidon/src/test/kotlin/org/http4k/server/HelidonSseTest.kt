package org.http4k.server

import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseServerContract
import org.junit.jupiter.api.Disabled

class HelidonSseTest : SseServerContract({ Helidon(it, Immediate) }) {

    @Disabled("not available in Helidon")
    override fun `can modify status`() = runBlocking {
        super.`can modify status`()
    }
}

