package org.http4k.filter

import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.sse.SseFilter
import org.http4k.sse.SseResponse

/**
 * Checks for the
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
