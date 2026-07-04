package org.http4k.filter

import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsResponse
import org.http4k.websocket.WsStatus

/**
 * Provides rebind protection for WebSocket upgrade requests.
 */
fun ServerFilters.WsRebindProtection(corsPolicy: CorsPolicy): WsFilter = WsFilter { next ->
    { req ->
        val origin = req.header("Origin")
        when {
            origin != null && !corsPolicy.originPolicy(origin) -> WsResponse.Companion { it.close(WsStatus.REFUSE) }
            else -> next(req)
        }
    }
}
