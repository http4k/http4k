package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.routing.sse.bind
import org.http4k.routing.websocket.bind
import org.http4k.sse.SseFilter
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsFilter
import org.http4k.websocket.WsHandler

fun poly(vararg routes: RoutingHandler<*, *, *>) = PolyHandler(
    routes.filterIsInstance<RoutingHttpHandler>().flatMap { it.routes }.takeIf { it.isNotEmpty() }?.let { RoutingHttpHandler(it) },
    routes.filterIsInstance<RoutingWsHandler>().flatMap { it.routes }.takeIf { it.isNotEmpty() }?.let { RoutingWsHandler(it) },
    routes.filterIsInstance<RoutingSseHandler>().flatMap { it.routes }.takeIf { it.isNotEmpty() }?.let { RoutingSseHandler(it) },
)

fun poly(routes: List<RoutingHandler<*, *, *>>) = poly(*routes.toTypedArray())

infix fun String.bind(poly: PolyHandler) =
    listOfNotNull(
        poly.http?.let {
            when (it) {
                is RoutingHttpHandler -> it.withBasePath(this)
                else -> this bind it
            }
        },
        poly.sse?.let {
            when (it) {
                is RoutingSseHandler -> it.withBasePath(this)
                else -> this bind it
            }
        },
        poly.ws?.let {
            when (it) {
                is RoutingWsHandler -> it.withBasePath(this)
                else -> this bind it
            }
        }
    )

/**
 * Detect the type of a Handler and apply the filter to it. Use this in the construction of PolyFilters
 */
fun Filter.thenPoly(next: HttpHandler) = when (next) {
    is RoutingHttpHandler -> next.withFilter(this)
    else -> this(next)
}


/**
 * Detect the type of a Handler and apply the filter to it. Use this in the construction of PolyFilters
 */
fun SseFilter.thenPoly(next: SseHandler) = when (next) {
    is RoutingSseHandler -> next.withFilter(this)
    else -> this(next)
}

/**
 * Detect the type of a Handler and apply the filter to it. Use this in the construction of PolyFilters
 */
fun WsFilter.thenPoly(next: WsHandler) = when (next) {
    is RoutingWsHandler -> next.withFilter(this)
    else -> this(next)
}
