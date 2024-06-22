package org.http4k.client

import io.helidon.webclient.api.WebClient
import org.http4k.core.BodyMode.Stream
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
import java.time.Duration

class HelidonClientStreamingTest : HttpClientContract(
    ::ApacheServer,
    HelidonClient(bodyMode = Stream),
    HelidonClient(
        WebClient.builder()
            .readTimeout(Duration.ofMillis(100))
            .build(),
        bodyMode = Stream
    )
) {
    override fun `supports query parameter list`() = assumeTrue(false, "Unsupported client feature")

    @Disabled
    override fun `fails with no protocol`() {
    }
}
