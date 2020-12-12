package org.http4k.client

import org.http4k.core.BodyMode
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Test

class JettyClientStreamingTest : HttpClientContract({ ApacheServer(it) }, JettyClient(bodyMode = BodyMode.Stream),
    JettyClient(bodyMode = BodyMode.Stream, requestModifier = timeout)) {

    @Test
    override fun `can forward response body to another request`() {
        // temporarily remove test
    }
}
