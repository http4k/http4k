package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.present
import org.http4k.client.ApacheClient
import org.junit.jupiter.api.Test
import java.util.*

class KtorNettyTest : ServerContract({ KtorNetty(Random().nextInt(1000) + 7456) }, ApacheClient()) {
    @Test
    override fun `ok when length already set`() {
    }

    override fun clientAddress(): Matcher<String?> = present()
}
