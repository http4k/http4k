package org.http4k.mcp.internal

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.hamkrest.hasStatus
import org.http4k.mcp.McpOptions
import org.junit.jupiter.api.Test

class McpClientSecurityTest {

    @Test
    fun `no security`() {
        assertSecurity { Response(OK) }
    }

    @Test
    fun `basic auth`() {
        assertSecurity("--basicAuth", "123:321") {
            assertThat(it.header("Authorization"), equalTo("Basic " + "123:321".base64Encode()))
            Response(OK)
        }
    }

    @Test
    fun `api key`() {
        assertSecurity("--apiKey", "12345") {
            assertThat(it.header("X-API-Key"), equalTo("12345"))
            Response(OK)
        }

        assertSecurity("--apiKey", "12345", "--apiKeyHeader", "foobar") {
            assertThat(it.header("foobar"), equalTo("12345"))
            Response(OK)
        }
    }

    @Test
    fun `bearer auth`() {
        assertSecurity("--bearerToken", "12345") {
            assertThat(it.header("Authorization"), equalTo("Bearer 12345"))
            Response(OK)
        }
    }

    private fun assertSecurity(vararg args: String, next: HttpHandler) {
        val filter = McpClientSecurity.from(McpOptions(args.toList().toTypedArray())).filter

        assertThat(filter.then(next)(Request(GET, "")), hasStatus(OK))
    }
}
