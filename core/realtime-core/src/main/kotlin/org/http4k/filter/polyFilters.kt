package org.http4k.filter

import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.sse.then

/**
 * Provides combination CORs and rebind protection
 */
fun ServerFilters.CorsAndRebindProtection(corsPolicy: CorsPolicy): PolyFilter = PolyFilter { next ->
    PolyHandler(
        http = next.http?.let { ServerFilters.Cors(corsPolicy).then(it) },
        sse = next.sse?.let { ServerFilters.SseRebindProtection(corsPolicy).then(it) }
    )
}
