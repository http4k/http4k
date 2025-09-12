package org.http4k.client

import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration

class FuelTest :
    HttpClientContract(::ApacheServer, Fuel(), Fuel(timeout = Duration.ofMillis(100))),
    HttpClientWithMemoryModeContract {

    @Test
    @Disabled
    override fun `can send multiple headers with same name`() {
        super.`can send multiple headers with same name`()
    }

    @Test
    override fun `random data then close is converted into 503`() = assumeTrue(false, "Unsupported feature")
}
