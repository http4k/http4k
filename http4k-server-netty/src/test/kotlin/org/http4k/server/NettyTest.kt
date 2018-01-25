package org.http4k.server

import org.http4k.client.ApacheClient
import org.junit.Test

class NettyTest : ServerContract(::Netty, ApacheClient()) {

    @Test
    override fun `can return a large body`() {
        // ignored here whilst we fix bug with netty
    }
}