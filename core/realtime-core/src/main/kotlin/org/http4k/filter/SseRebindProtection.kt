package org.http4k.filter

import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse

/**
 * Provides combination CORs and rebind protection for SSE requests
 */
fun ServerFilters.SseRebindProtection(corsPolicy: CorsPolicy): SseFilter = SseFilter { next ->
    { req ->
        val origin = req.header("Origin")
        when {
            // A rebinding attack always comes via a browser, which always sends an Origin; a missing Origin
            // is a non-browser/same-origin client, so allow it (matching HttpRebindProtection).
            origin != null && !corsPolicy.originPolicy(origin) -> SseResponse(FORBIDDEN, emptyList(), true) { it.close() }

            else -> {
                val corsHeaders = ServerFilters.Cors(corsPolicy).then { Response(OK) }(req).headers

                next(req)
                    .let { SseResponse(it.status, it.headers + corsHeaders, it.handled, it.consumer) }
            }
        }
    }
}
