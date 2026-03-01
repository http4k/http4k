package org.http4k.wiretap.home

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.util.Metrics
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.Test

class StatsTest {

    private val templates = Templates()
    private val renderer = DatastarElementRenderer(templates)

    private val stats = GetStats(
        transactionStore = TransactionStore.InMemory(),
        traceStore = TraceStore.InMemory(),
        inboundChaos = ChaosEngine(),
        outboundChaos = ChaosEngine(),
        mcpCapabilities = McpCapabilities("none"),
        meterRegistry = Metrics()
    )

    @Test
    fun `stats include uptime in HTTP response`() {
        val response = stats.http(renderer, templates)(Request(GET, "/stats"))
        val body = response.bodyString()
        assertThat(response.status, equalTo(OK))
        assertThat(body, containsSubstring("Uptime"))
    }

    @Test
    fun `stats include JVM metrics in HTTP response`() {
        val response = stats.http(renderer, templates)(Request(GET, "/stats"))

        assertThat(response.status, equalTo(OK))
        val body = response.bodyString()
        assertThat(body, containsSubstring("Heap Memory"))
        assertThat(body, containsSubstring("Threads"))
    }

    @Test
    fun `stats include JVM heap values in response`() {
        val response = stats.http(renderer, templates)(Request(GET, "/stats"))
        val body = response.bodyString()
        assertThat(body, containsSubstring("MB"))
        assertThat(body, containsSubstring("Process CPU"))
        assertThat(body, containsSubstring("GC Pauses"))
    }

    @Test
    fun `stats include class loader metrics in response`() {
        val response = stats.http(renderer, templates)(Request(GET, "/stats"))
        val body = response.bodyString()
        assertThat(body, containsSubstring("Classes"))
    }
}
