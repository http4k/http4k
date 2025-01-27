package org.http4k.routing

import org.http4k.core.PolyHandler

fun poly(vararg routes: RoutingHandler<*, *, *>) = PolyHandler(
    routes.filterIsInstance<RoutingHttpHandler>().flatMap { it.routes }.takeIf { it.isNotEmpty() }?.let { RoutingHttpHandler(it) },
    routes.filterIsInstance<RoutingWsHandler>().flatMap { it.routes }.takeIf { it.isNotEmpty() }?.let { RoutingWsHandler(it) },
    routes.filterIsInstance<RoutingSseHandler>().flatMap { it.routes }.takeIf { it.isNotEmpty() }?.let { RoutingSseHandler(it) },
)
