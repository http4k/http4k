package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class JettyTest : ServerContract(::Jetty, ClientForServerTesting()) {
    override fun requestScheme() = equalTo("http")

    @Disabled
    override fun `illegal url doesn't expose stacktrace`() {

    }

    @Test
    fun `returns status with pre-defined standardized description`() {
        val response = client(Request(Method.GET, "${baseUrl}/status-with-foobar-description"))

        assertThat(response.status.code, equalTo(201))
        assertThat(response.status.description, equalTo("Created"))
    }

    @Disabled
    override fun `treats multiple request headers as single item comma-separated list`() {

    }
}
