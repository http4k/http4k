package org.http4k.core

import org.http4k.routing.RoutingHttpHandler

typealias HttpHandler = (Request) -> Response

fun interface Filter : (HttpHandler) -> HttpHandler {
    companion object
}

val Filter.Companion.NoOp: Filter get() = Filter { next -> { next(it) } }

fun Filter.then(next: Filter): Filter = Filter { this(next(it)) }

fun Filter.then(next: HttpHandler): HttpHandler = { this(next)(it) }

fun Filter.then(routingHttpHandler: RoutingHttpHandler): RoutingHttpHandler = routingHttpHandler.withFilter(this)
