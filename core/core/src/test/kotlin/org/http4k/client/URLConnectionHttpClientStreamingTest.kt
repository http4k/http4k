package org.http4k.client

import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.server.SunHttp
import org.http4k.streaming.StreamingContract
import org.junit.jupiter.api.Assumptions.assumeTrue

class URLConnectionHttpClientStreamingTest : StreamingContract() {
    override fun serverConfig() = SunHttp(0)

    override fun createClient(): HttpHandler = URLConnectionHttpClient(bodyMode = Stream)

    override fun `can stream request`() =
        assumeTrue(false, "URLConnection buffers requests without a known content-length; only response streaming is supported")
}
