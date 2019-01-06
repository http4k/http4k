package org.http4k.server

import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class KtorCIOTest : ServerContract({ KtorCIO(Random().nextInt(1000) + 10000) }, ApacheClient()) {

    @BeforeEach
    fun forceStart() {
        client(Request(GET, baseUrl))
    }

    @Test
    override fun `ok when length already set`() {
    }

    @Test
    override fun `can start on port zero and then get the port`() {
    }
}