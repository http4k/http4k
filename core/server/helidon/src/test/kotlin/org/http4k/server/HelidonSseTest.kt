package org.http4k.server

import org.http4k.client.JavaHttpClient
import org.http4k.server.ServerConfig.StopMode.Immediate
import org.http4k.sse.SseServerContract
import org.junit.jupiter.api.Disabled

class HelidonSseTest : SseServerContract({ Helidon(it, Immediate) }, JavaHttpClient(), newThreadForClose = false) {

    @Disabled("not available in Helidon")
    override fun `when no http handler messages without the event stream header don't blow up`() {
        super.`when no http handler messages without the event stream header don't blow up`()
    }

    @Disabled("not available in Helidon")
    override fun `can modify status`() {
        super.`can modify status`()
    }
}
