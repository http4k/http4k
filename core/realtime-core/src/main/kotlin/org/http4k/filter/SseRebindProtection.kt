package org.http4k.filter

import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse

/**
 * Checks for the
 */
fun ServerFilters.SseRebindProtection(corsPolicy: CorsPolicy): SseFilter = SseFilter { next ->
    { request ->
        val origin = request.header("Origin")
        when {
            origin == null -> SseResponse(FORBIDDEN, emptyList(), true) { it.close() }
            !corsPolicy.originPolicy(origin) -> SseResponse(FORBIDDEN, emptyList(), true) { it.close() }
            else -> next(request)
        }
    }
}
