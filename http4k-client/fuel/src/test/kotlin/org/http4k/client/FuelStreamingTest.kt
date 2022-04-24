package org.http4k.client

import org.http4k.core.BodyMode.Stream
import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration

class FuelStreamingTest : HttpClientContract(
    ::ApacheServer,
    Fuel(bodyMode = Stream),
    Fuel(bodyMode = Stream, timeout = Duration.ofMillis(100)),
) {
    @Test
    @Disabled
    override fun `can send multiple headers with same name`() {
        super.`can send multiple headers with same name`()
    }
}
