package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Test
import java.util.*

class KtorCIOTest :
    ServerContract({ _, stopMode -> KtorCIO(Random().nextInt(1000) + 8745, stopMode) }, ClientForServerTesting()) {

    @Test
    override fun `ok when length already set`() {
    }

    @Test
    override fun `can start on port zero and then get the port`() {
    }

    override fun clientAddress(): Matcher<String?> = present()

    override fun requestScheme(): Matcher<String?> = equalTo("http")
}
