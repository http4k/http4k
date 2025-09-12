package org.http4k.client

import org.http4k.core.BodyMode
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

class OkHttpStreamingTest : HttpClientContract(::ApacheServer, OkHttp(bodyMode = BodyMode.Stream),
    OkHttp(timeout, bodyMode = BodyMode.Stream)) {
    @Test
    override fun `malformed response chunk is converted into 503`() = assumeTrue(false, "Unsupported feature")
}
