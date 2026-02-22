package org.http4k.filter

import org.http4k.core.HttpFilter
import org.http4k.core.HttpTransaction
import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.Response
import org.http4k.core.SseTransaction
import org.http4k.core.WsTransaction
import org.http4k.events.Events
import org.http4k.events.HttpEvent
import org.http4k.events.SseEvent
import org.http4k.events.WsEvent
import org.http4k.filter.ServerFilters.CatchAll.originalBehaviour
import org.http4k.routing.thenPoly
import org.http4k.security.Security
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsResponse
import java.time.Clock

/**
 * Filters that work with PolyHandler (multiple protocols)
 */
object PolyFilters {
    /**
     * Catch all filter for all protocols
     */
    fun CatchAll(
        onErrorHttp: (Throwable) -> Response = ::originalBehaviour,
        onErrorSse: (Throwable) -> SseResponse = ::originalSseBehaviour,
        onErrorWs: (Throwable) -> WsResponse = ::originalWsBehaviour
    ) = PolyFilter { next ->
        PolyHandler(
            http = next.http?.let { ServerFilters.CatchAll(onErrorHttp).thenPoly(it) },
            sse = next.sse?.let { ServerFilters.CatchAllSse(onErrorSse).thenPoly(it) },
            ws = next.ws?.let { ServerFilters.CatchAllWs(onErrorWs).thenPoly(it) }
        )
    }

    /**
     * Apply security tofor all protocols
     */
    fun Security(security: Security) = PolyFilter { next ->
        PolyHandler(
            http = next.http?.let { HttpFilter(security).thenPoly(it) },
            sse = next.sse?.let { SseFilter(security).thenPoly(it) },
            ws = next.ws?.let { WsFilter(security).thenPoly(it) }
        )
    }

    /**
     * Report transactions for all protocols
     */
    fun ReportTransaction(
        clock: Clock = Clock.systemUTC(),
        httpTransactionLabeler: HttpTransactionLabeler = { it },
        sseTransactionLabeler: SseTransactionLabeler = { it },
        wsTransactionLabeler: WsTransactionLabeler = { it },
        recordHttpFn: (HttpTransaction) -> Unit,
        recordSseFn: (SseTransaction) -> Unit,
        recordWsFn: (WsTransaction) -> Unit
    ) = PolyFilter { next ->
        PolyHandler(
            http = next.http?.let {
                ResponseFilters.ReportHttpTransaction(clock, httpTransactionLabeler, recordHttpFn).thenPoly(it)
            },
            sse = next.sse?.let {
                ResponseFilters.ReportSseTransaction(clock, sseTransactionLabeler, recordSseFn).thenPoly(it)
            },
            ws = next.ws?.let {
                ResponseFilters.ReportWsTransaction(clock, wsTransactionLabeler, recordWsFn).thenPoly(it)
            }
        )
    }

    /**
     * Report transaction events for all protocols
     */
    fun ReportTransaction(events: Events) = PolyFilter { next ->
        PolyHandler(
            http = next.http?.let {
                ResponseFilters.ReportHttpTransaction { events(HttpEvent.Incoming(it)) }.thenPoly(it)
            },
            sse = next.sse?.let {
                ResponseFilters.ReportSseTransaction { events(SseEvent.Incoming(it)) }.thenPoly(it)
            },
            ws = next.ws?.let {
                ResponseFilters.ReportWsTransaction { events(WsEvent.Incoming(it)) }.thenPoly(it)
            }
        )
    }
}
