package org.http4k.wiretap.acceptance

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.model.ToolName
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.util.FixedClock
import org.http4k.util.PortBasedTest
import org.http4k.wiretap.Wiretap
import org.http4k.wiretap.WiretappedUriProvider
import org.http4k.wiretap.util.Json
import org.junit.jupiter.api.Test

interface WiretapSmokeContract : PortBasedTest {

    val uriProvider: WiretappedUriProvider
    val testRequest: Request

    @Test
    fun `can boot and count tools`() {
        Wiretap(uriProvider = uriProvider).testMcpClient(Request(POST, "_wiretap/mcp")).use {
            assertThat(it.tools().list().map { it.size }.orThrowIt(), equalTo(16))
        }
    }

    @Test
    fun `transactions through wiretap are stored`() {
        val wiretap = Wiretap(
            clock = FixedClock,
            uriProvider = uriProvider
        )

        wiretap.testMcpClient(Request(POST, "_wiretap/mcp")).use {
            wiretap.http!!(testRequest)

            val call = it.tools().call(ToolName.of("list_transactions")).orThrowIt()

            val calls = (call as ToolResponse.Ok).content!![0] as Text
            val elements = Json.elements(Json.parse(calls.text))
            assertThat(elements.size, greaterThan(0))
        }
    }
}

fun <T> McpResult<T>.orThrowIt() = when (this) {
    is Success<T> -> value
    is Failure<*> -> throw Exception(toString())
}
