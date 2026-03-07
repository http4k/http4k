package org.http4k.wiretap

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.wiretap.domain.BodyHydration
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TrafficMetrics
import org.http4k.wiretap.domain.TransactionStore
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant.EPOCH
import java.time.ZoneOffset.UTC

class ProxyTest {

    private val clock = Clock.fixed(EPOCH, UTC)
    private val transactions = TransactionStore.InMemory()
    private val trafficMetrics = TrafficMetrics(SimpleMeterRegistry(), clock = clock)

    private fun proxy(
        bodyHydration: BodyHydration = BodyHydration.None,
        httpClient: (Request) -> Response = { Response(OK).body("upstream") },
        inboundChaos: ChaosEngine = ChaosEngine(),
        outboundChaos: ChaosEngine = ChaosEngine(),
        sanitise: (org.http4k.core.HttpTransaction) -> org.http4k.core.HttpTransaction? = { it }
    ) = Proxy(
        bodyHydration = bodyHydration,
        httpClient = httpClient,
        clock = clock,
        traces = TraceStore.InMemory(),
        transactions = transactions,
        trafficMetrics = trafficMetrics,
        inboundChaos = inboundChaos,
        outboundChaos = outboundChaos,
        sanitise = sanitise,
        appBuilder = { _, _ -> Uri.of("http://localhost:9000") }
    )

    @Test
    fun `inbound request is recorded with Direction Inbound`() {
        val proxy = proxy()
        proxy.routing(Request(GET, "/test"))

        val recorded = transactions.list()
        assertThat(recorded, hasSize(equalTo(1)))
        assertThat(recorded.first().direction, equalTo(Inbound))
    }

    @Test
    fun `outbound request is recorded with Direction Outbound`() {
        val proxy = proxy()
        proxy.outboundHttp(Request(GET, "http://example.com/test"))

        val recorded = transactions.list()
        assertThat(recorded, hasSize(equalTo(1)))
        assertThat(recorded.first().direction, equalTo(Outbound))
    }

    @Test
    fun `sanitise filter can suppress recording`() {
        val proxy = proxy(sanitise = { null })
        proxy.routing(Request(GET, "/test"))

        assertThat(transactions.list(), hasSize(equalTo(0)))
    }

    @Test
    fun `sanitise filter transforms transaction before recording`() {
        val proxy = proxy(sanitise = { tx ->
            tx.copy(request = tx.request.removeHeader("Authorization"))
        })
        proxy.routing(Request(GET, "/test").header("Authorization", "Bearer secret"))

        val recorded = transactions.list().first()
        assertThat(recorded.transaction.request.header("Authorization"), absent())
    }

    @Test
    fun `chaos filter is applied to inbound proxied requests`() {
        val inboundChaos = ChaosEngine()
        inboundChaos.enable(org.http4k.chaos.ChaosBehaviours.ReturnStatus(INTERNAL_SERVER_ERROR))

        val proxy = proxy(inboundChaos = inboundChaos)
        val response = proxy.routing(Request(GET, "/test"))

        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `chaos filter is applied to outbound requests`() {
        val outboundChaos = ChaosEngine()
        outboundChaos.enable(org.http4k.chaos.ChaosBehaviours.ReturnStatus(INTERNAL_SERVER_ERROR))

        val proxy = proxy(outboundChaos = outboundChaos)
        val response = proxy.outboundHttp(Request(GET, "http://example.com/test"))

        assertThat(response.status, equalTo(INTERNAL_SERVER_ERROR))
    }
}
