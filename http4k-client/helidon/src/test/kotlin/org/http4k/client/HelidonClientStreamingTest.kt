package org.http4k.client

import io.helidon.common.socket.SocketOptions
import io.helidon.nima.webclient.WebClient
import org.http4k.core.BodyMode.Stream
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.time.Duration

class HelidonClientStreamingTest : HttpClientContract(
    ::ApacheServer,
    HelidonClient(bodyMode = Stream),
    HelidonClient(
        WebClient.builder()
            .channelOptions(
                SocketOptions.builder()
                    .readTimeout(Duration.ofMillis(100))
                    .build()
            )
            .build(),
        bodyMode = Stream
    )
){
    override fun `supports query parameter list`() = assumeTrue(false, "Unsupported client feature")
}
