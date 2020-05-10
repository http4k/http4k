package org.http4k.server

import org.http4k.client.ApacheClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class KtorNettyTest : ServerContract({ KtorNetty(Random().nextInt(1000) + 10000) }, ApacheClient()) {
    @Test
    override fun `ok when length already set`() {
    }
}
