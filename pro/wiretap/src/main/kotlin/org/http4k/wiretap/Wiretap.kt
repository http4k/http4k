/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.chaos.ChaosEngine
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.routing.bind
import org.http4k.routing.orElse
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.security.NoSecurity
import org.http4k.security.Security
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.chaos.Chaos
import org.http4k.wiretap.client.InboundClient
import org.http4k.wiretap.client.OutboundClient
import org.http4k.wiretap.domain.BodyHydration
import org.http4k.wiretap.domain.BodyHydration.All
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TrafficMetrics
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.ViewStore
import org.http4k.wiretap.home.GetStats
import org.http4k.wiretap.home.McpCapabilities
import org.http4k.wiretap.mcp.Mcp
import org.http4k.wiretap.mcp_api.AnalyzeTrafficPrompt
import org.http4k.wiretap.mcp_api.DebugRequestPrompt
import org.http4k.wiretap.mcp_api.WiretapMcp
import org.http4k.wiretap.openapi.OpenApi
import org.http4k.wiretap.otel.OTel
import org.http4k.wiretap.proxy.Proxy
import org.http4k.wiretap.traffic.Traffic
import org.http4k.wiretap.util.CatchAndReportErrors
import org.http4k.wiretap.util.Metrics
import org.http4k.wiretap.util.Templates
import java.security.SecureRandom
import java.time.Clock
import java.util.Random
import kotlin.concurrent.fixedRateTimer

/**
 * Wiretap is a console for monitoring and testing http4k applications. It wraps a http4k application and records
 * all requests and responses, allowing you to monitor what is going on inside the app.
 */
object Wiretap {
    operator fun invoke(
        target: WiretapTarget,
        transactionStore: TransactionStore = TransactionStore.InMemory(),
        traceStore: TraceStore = TraceStore.InMemory(),
        logStore: LogStore = LogStore.InMemory(),
        viewStore: ViewStore = ViewStore.InMemory(),
        security: Security = NoSecurity,
        mcpOptions: WiretapMcpServerOptions = WiretapMcpServerOptions.default,
        httpClient: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
        sanitise: (HttpTransaction) -> HttpTransaction? = { it },
        clock: Clock = Clock.systemUTC(),
        random: Random = SecureRandom(byteArrayOf()),
        bodyHydration: BodyHydration = All,
    ): PolyHandler {

        val html = Templates()
        val renderer = DatastarElementRenderer(html)

        val inboundChaos = ChaosEngine()
        val outboundChaos = ChaosEngine()

        val meterRegistry = Metrics()
        val trafficMetrics = TrafficMetrics(meterRegistry, clock = clock)

        fixedRateTimer("wiretap-snapshot", daemon = true, period = 5_000L) { trafficMetrics.snapshot() }

        val (wiretapped, proxy, outboundHttp) = Proxy(
            target,
            bodyHydration,
            httpClient,
            clock,
            random,
            traceStore,
            logStore,
            transactionStore,
            trafficMetrics,
            inboundChaos,
            outboundChaos,
            sanitise,
        )

        val prompts = listOf(AnalyzeTrafficPrompt(), DebugRequestPrompt())

        val baseFunctions = listOf(
            Traffic(transactionStore, viewStore, clock),
            Chaos(inboundChaos, outboundChaos),
            OTel(traceStore, logStore, transactionStore, clock),
            InboundClient(clock, transactionStore, proxy),
            OutboundClient(outboundHttp, clock, transactionStore),
            OpenApi(),
            Mcp(wiretapped, mcpOptions.path, httpClient, proxy)
        )

        val allCapabilities = prompts + baseFunctions.flatMap { it.mcp() }

        val mcpCapabilities = McpCapabilities(
            mcpOptions.security.name,
            allCapabilities.count { it is ToolCapability } + 1
        )

        val allFunctions = baseFunctions +
            GetStats(trafficMetrics, traceStore, inboundChaos, outboundChaos, mcpCapabilities, meterRegistry)

        val mcpRoutes = "/_wiretap" bind WiretapMcp(mcpOptions.security, allFunctions)

        val http = CatchAndReportErrors()
            .then(
                routes(
                    WiretapUi(renderer, html, allFunctions, security),
                    orElse bind proxy
                )
            )

        return poly(listOf(http) + mcpRoutes)
    }

}

