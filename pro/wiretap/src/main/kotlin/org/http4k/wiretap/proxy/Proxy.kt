/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.proxy

import org.http4k.chaos.ChaosEngine
import org.http4k.core.Body
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpTransaction
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ResponseFilters
import org.http4k.routing.bind
import org.http4k.routing.orElse
import org.http4k.routing.routes
import org.http4k.wiretap.WiretapContext
import org.http4k.wiretap.WiretapTarget
import org.http4k.wiretap.Wiretapped
import org.http4k.wiretap.domain.BodyHydration
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TrafficMetrics
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.otel.WiretapOpenTelemetry
import java.time.Clock

data class ProxyHandlers(
    val wiretapped: Wiretapped,
    val http: HttpHandler,
    val outboundHttp: HttpHandler
)

fun Proxy(
    target: WiretapTarget,
    bodyHydration: BodyHydration,
    httpClient: HttpHandler,
    clock: Clock,
    traces: TraceStore,
    logs: LogStore,
    transactions: TransactionStore,
    trafficMetrics: TrafficMetrics,
    inboundChaos: ChaosEngine,
    outboundChaos: ChaosEngine,
    sanitise: (HttpTransaction) -> HttpTransaction?,
): ProxyHandlers {

    val bufferRequest = Filter { next ->
        {
            if (bodyHydration(it)) it.body.payload
            next(it)
        }
    }

    val bufferResponse = Filter { next ->
        {
            next(it).let { response ->
                if (bodyHydration(response)) response.body(Body(response.body.payload))
                else response
            }
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

    val setup = WiretapContext(outboundHttp) { name -> WiretapOpenTelemetry(traces, logs, name) }
    val wiretapped = target(setup)

    val inboundHandler = recordTransaction(Inbound)
        .then(inboundChaos)
        .then(wiretapped.using(httpClient))


    return ProxyHandlers(
        wiretapped,
        http = routes(orElse bind inboundHandler),
        outboundHttp = ClientFilters.FollowRedirects().then(outboundHttp)
    )
}
