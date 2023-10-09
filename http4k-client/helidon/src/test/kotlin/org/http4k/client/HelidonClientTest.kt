package org.http4k.client

import io.helidon.common.socket.SocketOptions
import io.helidon.nima.webclient.WebClient
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Assumptions
import java.time.Duration

class HelidonClientTest : HttpClientContract(
    ::ApacheServer, HelidonClient(),
    HelidonClient(WebClient.builder()
        .channelOptions(SocketOptions.builder()
            .readTimeout(Duration.ofMillis(100))
            .build())
        .build())
){
    override fun `supports query parameter list`() = Assumptions.assumeTrue(false, "Unsupported client feature")
}


