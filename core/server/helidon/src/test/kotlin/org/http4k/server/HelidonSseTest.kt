package org.http4k.server

import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseServerContract
import org.junit.jupiter.api.Disabled

class HelidonSseTest : SseServerContract({ Helidon(it, Immediate) }) {

    @Disabled("Helidon SSE sink always responds with 200")
    override fun `can modify status`() {
        super.`can modify status`()
    }
}

