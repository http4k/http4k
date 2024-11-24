package org.http4k.server

import org.http4k.client.JavaHttpClient
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseServerContract
import org.junit.jupiter.api.Test

class HelidonSseTest : SseServerContract({ Helidon(it, Immediate) }, JavaHttpClient()) {
    @Test
    override fun `supports methods`() {
        super.`supports methods`()
    }

}
