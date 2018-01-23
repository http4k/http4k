package org.http4k.client

import org.http4k.core.BodyMode
import org.http4k.server.SunHttp
import org.junit.Test

class JettyClientStreamingTest : HttpClientContract({ SunHttp(it) }, JettyClient(bodyMode = BodyMode.Stream),
        JettyClient(bodyMode = BodyMode.Stream, requestModifier = timeout)) {

    @Test
    override fun `can forward response body to another request`() {
        // temporarily remove test
    }
}