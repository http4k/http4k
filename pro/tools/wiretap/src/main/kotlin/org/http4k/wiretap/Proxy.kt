package org.http4k.wiretap

import org.http4k.chaos.ChaosEngine
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ResponseFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.orElse
import org.http4k.routing.routes
import org.http4k.wiretap.domain.BodyHydration
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TrafficMetrics
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.otel.WiretapOpenTelemetry
import java.time.Clock

data class ProxyHandlers(
    val routing: RoutingHttpHandler,
    val outboundHttp: HttpHandler
)

fun Proxy(
    bodyHydration: BodyHydration,
    httpClient: HttpHandler,
    clock: Clock,
    traces: TraceStore,
    transactions: TransactionStore,
    trafficMetrics: TrafficMetrics,
    inboundChaos: ChaosEngine,
    outboundChaos: ChaosEngine,
    sanitise: (HttpTransaction) -> HttpTransaction?,
    appBuilder: WiretapAppBuilder
): ProxyHandlers {

    val bufferRequest = Filter { next ->
        {
            if (bodyHydration(it)) it.body.payload
            next(it)
        }
    }

    val bufferResponse = Filter { next ->
        {
            next(it)
                .also { if (bodyHydration(it)) it.body.payload }
        }
    }

    fun recordTransaction(direction: Direction) = bufferRequest
        .then(
            ResponseFilters.ReportHttpTransaction(clock) { tx ->
                sanitise(tx)?.let {
                    transactions.record(it, direction)
                    trafficMetrics.record(it, direction)
                }
            })
        .then(bufferResponse)

    val outboundHttp = recordTransaction(Outbound).then(outboundChaos).then(httpClient)

    val uri = appBuilder(outboundHttp, WiretapOpenTelemetry(traces), clock)

    return ProxyHandlers(
        routing = routes(
            orElse bind
                recordTransaction(Inbound)
                    .then(inboundChaos)
                    .then(ClientFilters.SetBaseUriFrom(uri))
                    .then(ClientFilters.FollowRedirects())
                    .then(httpClient)
        ),
        outboundHttp = ClientFilters.FollowRedirects().then(outboundHttp)
    )
}
