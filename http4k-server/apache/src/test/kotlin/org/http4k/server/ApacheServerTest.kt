package org.http4k.server

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NO_CONTENT
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ApacheServerTest : ServerContract({ port -> ApacheServer(port, canonicalHostname = "localhost") },
    ApacheClient(),
    Method.entries.filter { it != Method.PURGE }.toTypedArray()) {

    override fun requestScheme(): Matcher<String?> = equalTo("http")

    @Test
    @Disabled("unsupported by the underlying server")
    override fun `return 204 no content`() {
        assertThat(client(Request(GET, "$baseUrl/no-content")).status, equalTo(NO_CONTENT))
    }
}

