package org.http4k.client

import io.helidon.common.socket.SocketOptions
import io.helidon.nima.webclient.WebClient
import org.http4k.core.BodyMode.Stream
import org.http4k.server.ApacheServer
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
)
