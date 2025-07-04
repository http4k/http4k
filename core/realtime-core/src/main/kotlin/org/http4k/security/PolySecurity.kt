package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.PolyFilter
import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.sse.SseFilter
import org.http4k.sse.then
import org.http4k.websocket.WsFilter
import org.http4k.websocket.then

/**
 * Applies common security to all protocols of a PolyHandler
 */
fun ServerFilters.PolySecurity(security: Security) = PolyFilter { next ->
    PolyHandler(
        http = next.http?.let { Filter(security).then(it) },
        sse = next.sse?.let { SseFilter(security).then(it) },
        ws = next.ws?.let { WsFilter(security).then(it) }
    )
}
