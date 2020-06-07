package org.http4k.server

import org.http4k.client.ApacheClient
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.util.Random

class KtorNettyTest : ServerContract({ KtorNetty(Random().nextInt(1000) + 7456) }, ApacheClient()) {
    @Test
    override fun `ok when length already set`() {
    }

    override fun clientAddress(): String? = InetAddress.getLocalHost().hostName
}
