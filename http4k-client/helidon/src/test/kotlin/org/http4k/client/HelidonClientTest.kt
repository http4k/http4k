package org.http4k.client

import io.helidon.webclient.api.WebClient
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled
import java.time.Duration

class HelidonClientTest : HttpClientContract(
    ::ApacheServer, HelidonClient(),
    HelidonClient(WebClient.builder().readTimeout(Duration.ofMillis(100)).build())
) {
    override fun `supports query parameter list`() = Assumptions.assumeTrue(false, "Unsupported client feature")

    @Disabled
    override fun `fails with no protocol`() {
    }
}


