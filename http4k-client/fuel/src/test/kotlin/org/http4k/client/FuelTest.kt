package org.http4k.client

import org.http4k.server.ApacheServer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class FuelTest : HttpClientContract(::ApacheServer, Fuel(), Fuel()) {
    @Test
    @Disabled
    override fun `can send multiple headers with same name`() {
        super.`can send multiple headers with same name`()
    }
}


