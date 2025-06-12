package org.http4k.server

import org.junit.jupiter.api.Disabled

@Disabled("Clash of Netty version")
class RatpackTest : ServerContract(::Ratpack, ClientForServerTesting()) {

    override fun `ok when length already set`() {
    }
}

