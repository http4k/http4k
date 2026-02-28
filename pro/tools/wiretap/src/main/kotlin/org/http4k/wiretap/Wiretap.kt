package org.http4k.wiretap

import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.chaos.ChaosEngine
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
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
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.domain.ViewStore
import org.http4k.wiretap.home.GetStats
import org.http4k.wiretap.mcp.WiretapMcp
import org.http4k.wiretap.openapi.OpenApi
import org.http4k.wiretap.otel.OTel
import org.http4k.wiretap.traffic.Traffic
import org.http4k.wiretap.traffic.TrafficStream
import org.http4k.wiretap.util.Templates
import java.time.Clock

/**
 * Wiretap is a tool for debugging http4k applications. It wraps a http4k application and records
 * all requests and responses, allowing you to monitor what is going on inside the app.
 */
object Wiretap {
    fun Http(
        transactionStore: TransactionStore = TransactionStore.InMemory(),
        traceStore: TraceStore = TraceStore.InMemory(),
        viewStore: ViewStore = ViewStore.InMemory(),
        httpClient: HttpHandler = JavaHttpClient(),
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

        val (proxy, outboundHttp) = Proxy(
            bodyHydration,
            httpClient,
            clock,
            traceStore,
            transactionStore,
            inboundChaos,
            outboundChaos,
            sanitise,
            appBuilder
        )

        val functions = listOf(
            Traffic(transactionStore, viewStore),
            Chaos(inboundChaos, outboundChaos),
            OTel(traceStore),
            InboundClient(clock, transactionStore, proxy),
            OutboundClient(outboundHttp, clock, transactionStore),
            GetStats(clock, transactionStore, traceStore, inboundChaos, outboundChaos),
            OpenApi()
        )

        val mcpRoutes = "/_wiretap" bind WiretapMcp("http4k-wiretap", mcpSecurity, functions)

        return poly(
            listOf(
                ServerFilters.CatchAll().then(
                    routes(WiretapUi(renderer, templates, functions), proxy)
                ),
                "/_wiretap/traffic" bind TrafficStream(transactionStore, renderer)
            ) + mcpRoutes
        )
    }
}
