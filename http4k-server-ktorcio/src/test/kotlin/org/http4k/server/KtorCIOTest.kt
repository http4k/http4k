package org.http4k.server

import org.http4k.client.ApacheClient
import org.junit.jupiter.api.Test
import java.util.Random

class KtorCIOTest : ServerContract({ KtorCIO(Random().nextInt(1000) + 8000) }, ApacheClient()) {

    @Test
    override fun `ok when length already set`() {
    }

    @Test
    override fun `can start on port zero and then get the port`() {
    }
}