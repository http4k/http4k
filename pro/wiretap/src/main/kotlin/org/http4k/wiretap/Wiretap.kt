package org.http4k.wiretap

import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.chaos.ChaosEngine
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.orElse
import org.http4k.routing.poly
import org.http4k.routing.routes
import org.http4k.routing.sse.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.chaos.Chaos
import org.http4k.wiretap.client.InboundClient
import org.http4k.wiretap.client.OutboundClient
import org.http4k.wiretap.domain.BodyHydration
import org.http4k.wiretap.domain.BodyHydration.All
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
import org.http4k.wiretap.traffic.Traffic
import org.http4k.wiretap.traffic.TrafficStream
import org.http4k.wiretap.util.Metrics
import org.http4k.wiretap.util.Templates
import java.time.Clock
import kotlin.concurrent.fixedRateTimer

/**
 * Wiretap is a tool for debugging http4k applications. It wraps a http4k application and records
 * all requests and responses, allowing you to monitor what is going on inside the app.
 */
object Wiretap {
    operator fun invoke(
        uri: Uri,
        httpClient: HttpHandler = JavaHttpClient(responseBodyMode = Stream)
    ) =
        this(httpClient = httpClient, appBuilder = { _, _, _ -> uri })

    operator fun invoke(
        transactionStore: TransactionStore = TransactionStore.InMemory(),
        traceStore: TraceStore = TraceStore.InMemory(),
        viewStore: ViewStore = ViewStore.InMemory(),
        httpClient: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
        mcpSecurity: McpSecurity = NoMcpSecurity,
        clock: Clock = Clock.systemUTC(),
        sanitise: (HttpTransaction) -> HttpTransaction? = { it },
        bodyHydration: BodyHydration = All,
        appBuilder: WiretapAppBuilder
    ): PolyHandler {

        val templates = Templates()
        val renderer = DatastarElementRenderer(templates)

        val inboundChaos = ChaosEngine()
        val outboundChaos = ChaosEngine()

        val meterRegistry = Metrics()
        val trafficMetrics = TrafficMetrics(meterRegistry, clock = clock)

        fixedRateTimer("wiretap-snapshot", daemon = true, period = 5_000L) { trafficMetrics.snapshot() }

        val (uri, proxy, outboundHttp) = Proxy(
            bodyHydration,
            httpClient,
            clock,
            traceStore,
            transactionStore,
            trafficMetrics,
            inboundChaos,
            outboundChaos,
            sanitise,
            appBuilder
        )

        val prompts = listOf(AnalyzeTrafficPrompt(), DebugRequestPrompt())

        val baseFunctions = listOf(
            Traffic(transactionStore, viewStore),
            Chaos(inboundChaos, outboundChaos),
            OTel(traceStore),
            InboundClient(clock, transactionStore, proxy),
            OutboundClient(outboundHttp, clock, transactionStore),
            OpenApi(),
            Mcp(uri, httpClient, proxy)
        )

        val allCapabilities = prompts + baseFunctions.flatMap { it.mcp() }

        val mcpCapabilities = McpCapabilities(
            mcpSecurity.name,
            allCapabilities.count { it is ToolCapability } + 1
        )

        val functions = baseFunctions +
            GetStats(trafficMetrics, traceStore, inboundChaos, outboundChaos, mcpCapabilities, meterRegistry)

        val mcpRoutes = "/_wiretap" bind WiretapMcp("http4k-wiretap", mcpSecurity, functions)

        val httpRoutes = listOf(
            ServerFilters.CatchAll()
                .then(
                    routes(
                        WiretapUi(renderer, templates, functions),
                        orElse bind proxy
                    )
                ),
            "/_wiretap/traffic" bind TrafficStream(transactionStore, renderer),
        )
        return poly(
            httpRoutes + mcpRoutes
        )
    }
}

