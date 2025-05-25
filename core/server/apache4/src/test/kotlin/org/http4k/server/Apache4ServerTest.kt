package org.http4k.server

import org.http4k.core.Method
import org.junit.jupiter.api.Disabled

class Apache4ServerTest : ServerContract(::Apache4Server, ClientForServerTesting(),
    Method.entries.filter { it != Method.PURGE }.toTypedArray()) {

    @Disabled("resets connection for some reason")
    override fun `can act as a simple proxy`() = runBlocking {
        super.`can act as a simple proxy`()
    }
}
