package org.http4k.server

import org.http4k.client.ApacheClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class KtorCIOTest : ServerContract({ KtorCIO(Random().nextInt(1000) + 8745) }, ApacheClient()) {

    @BeforeEach
    fun sleepForABitBecauseStartupIsCrushinglySlow() {
        Thread.sleep(1000)
    }

    @Test
    override fun `ok when length already set`() {
    }
}
