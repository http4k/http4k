package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.junit.jupiter.api.Disabled

class ApacheServerTest : ServerContract(
    ::ApacheServer,
    ClientForServerTesting(),
    Method.entries.filter { it != Method.PURGE }.toTypedArray()
) {

    override fun requestScheme(): Matcher<String?> = equalTo("http")

    @Disabled("Currently returns a 400")
    override fun `can act as a simple proxy`() = runBlocking {
        super.`can act as a simple proxy`()
    }
}

