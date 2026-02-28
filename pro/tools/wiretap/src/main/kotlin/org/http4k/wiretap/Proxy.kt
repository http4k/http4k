package org.http4k.wiretap

import org.http4k.chaos.ChaosEngine
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.then
import org.http4k.filter.ResponseFilters
import org.http4k.filter.ResponseFilters.ReportHttpTransaction.invoke
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.orElse
import org.http4k.routing.routes
import org.http4k.wiretap.domain.BodyHydration
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.TraceStore
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
                sanitise(tx)?.let { transactions.record(it, direction) }
            })
        .then(bufferResponse)

    val outboundHttp = recordTransaction(Direction.Outbound).then(outboundChaos).then(httpClient)

    return ProxyHandlers(
        routing = routes(
            orElse bind recordTransaction(Direction.Inbound)
                .then(inboundChaos)
                .then(appBuilder(outboundHttp, WiretapOpenTelemetry(traces), clock))
        ),
        outboundHttp = outboundHttp
    )
}
