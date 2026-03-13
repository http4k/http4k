/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.proxy

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.chaos.ChaosBehaviours
import org.http4k.chaos.ChaosEngine
import org.http4k.core.Body
import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.wiretap.domain.BodyHydration
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TrafficMetrics
import org.http4k.wiretap.domain.TransactionStore
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ProxyTest {

    private val clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
    private val transactions = TransactionStore.InMemory()
    private val trafficMetrics = TrafficMetrics(SimpleMeterRegistry(), clock = clock)

    private fun proxy(
        bodyHydration: BodyHydration = BodyHydration.None,
        httpClient: (Request) -> Response = { Response(Status.OK).body("upstream") },
        inboundChaos: ChaosEngine = ChaosEngine(),
        outboundChaos: ChaosEngine = ChaosEngine(),
        sanitise: (HttpTransaction) -> HttpTransaction? = { it }
    ) = Proxy(
        bodyHydration = bodyHydration,
        httpClient = httpClient,
        clock = clock,
        traces = TraceStore.InMemory(),
        logs = LogStore.InMemory(),
        transactions = transactions,
        trafficMetrics = trafficMetrics,
        inboundChaos = inboundChaos,
        outboundChaos = outboundChaos,
        sanitise = sanitise,
        uriProvider = { _, _ -> Uri.of("http://localhost:9000") }
    )

    @Test
    fun `inbound request is recorded with Direction Inbound`() {
        val proxy = proxy()
        proxy.http(Request(Method.GET, "/test"))

        val recorded = transactions.list()
        assertThat(recorded, hasSize(equalTo(1)))
        assertThat(recorded.first().direction, equalTo(Direction.Inbound))
    }

    @Test
    fun `outbound request is recorded with Direction Outbound`() {
        val proxy = proxy()
        proxy.outboundHttp(Request(Method.GET, "http://example.com/test"))

        val recorded = transactions.list()
        assertThat(recorded, hasSize(equalTo(1)))
        assertThat(recorded.first().direction, equalTo(Direction.Outbound))
    }

    @Test
    fun `sanitise filter can suppress recording`() {
        val proxy = proxy(sanitise = { null })
        proxy.http(Request(Method.GET, "/test"))

        assertThat(transactions.list(), hasSize(equalTo(0)))
    }

    @Test
    fun `sanitise filter transforms transaction before recording`() {
        val proxy = proxy(sanitise = { tx ->
            tx.copy(request = tx.request.removeHeader("Authorization"))
        })
        proxy.http(Request(Method.GET, "/test").header("Authorization", "Bearer secret"))

        val recorded = transactions.list().first()
        assertThat(recorded.transaction.request.header("Authorization"), absent())
    }

    @Test
    fun `chaos filter is applied to inbound proxied requests`() {
        val inboundChaos = ChaosEngine()
        inboundChaos.enable(ChaosBehaviours.ReturnStatus(Status.INTERNAL_SERVER_ERROR))

        val proxy = proxy(inboundChaos = inboundChaos)
        val response = proxy.http(Request(Method.GET, "/test"))

        assertThat(response.status, equalTo(Status.INTERNAL_SERVER_ERROR))
    }

    @Test
    fun `records transaction with stream bodies intact`() {
        val proxy = proxy(
            bodyHydration = BodyHydration.All,
            httpClient = { Response(Status.OK).body(Body("response-body".byteInputStream())) }
        )
        proxy.http(Request(Method.GET, "/test").body(Body("request-body".byteInputStream())))

        val recorded = transactions.list()
        assertThat(recorded, hasSize(equalTo(1)))
        assertThat(recorded.first().transaction.request.bodyString(), equalTo("request-body"))
        assertThat(recorded.first().transaction.response.bodyString(), equalTo("response-body"))
    }

    @Test
    fun `chaos filter is applied to outbound requests`() {
        val outboundChaos = ChaosEngine()
        outboundChaos.enable(ChaosBehaviours.ReturnStatus(Status.INTERNAL_SERVER_ERROR))

        val proxy = proxy(outboundChaos = outboundChaos)
        val response = proxy.outboundHttp(Request(Method.GET, "http://example.com/test"))

        assertThat(response.status, equalTo(Status.INTERNAL_SERVER_ERROR))
    }
}
