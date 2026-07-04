package org.http4k.filter

import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.routing.thenPoly

/**
 * Provides combination CORs and rebind protection for HTTP, SSE and WebSocket requests
 */
fun PolyFilters.CorsAndRebindProtection(corsPolicy: CorsPolicy): PolyFilter = PolyFilter { next ->
    PolyHandler(
        http = next.http?.let { ServerFilters.HttpRebindProtection(corsPolicy).thenPoly(it) },
        sse = next.sse?.let { ServerFilters.SseRebindProtection(corsPolicy).thenPoly(it) },
        ws = next.ws?.let { ServerFilters.WsRebindProtection(corsPolicy).thenPoly(it) }
    )
}
