package org.http4k.filter

import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.routing.thenPoly
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse

/**
 * Provides combination CORs and rebind protection for SSE requests
 */
fun ServerFilters.SseRebindProtection(corsPolicy: CorsPolicy): SseFilter = SseFilter { next ->
    { req ->
        val origin = req.header("Origin")
        when {
            origin == null -> SseResponse(FORBIDDEN, emptyList(), true) { it.close() }
            !corsPolicy.originPolicy(origin) -> SseResponse(FORBIDDEN, emptyList(), true) { it.close() }
            else -> {
                val corsHeaders = ServerFilters.Cors(corsPolicy).then { Response(OK) }(req).headers

                next(req)
                    .let { SseResponse(it.status, it.headers + corsHeaders, it.handled, it.consumer) }
            }
        }
    }
}

/**
 * Provides combination CORs and rebind protection for HTTP and SSE requests
 */
fun PolyFilters.CorsAndRebindProtection(corsPolicy: CorsPolicy): PolyFilter = PolyFilter { next ->
    PolyHandler(
        http = next.http?.let { ServerFilters.Cors(corsPolicy).thenPoly(it) },
        sse = next.sse?.let { ServerFilters.SseRebindProtection(corsPolicy).thenPoly(it) }
    )
}
