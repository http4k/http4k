package org.http4k.core

import org.http4k.routing.RoutingHttpHandler

typealias HandleRequest = (Request) -> Response

fun interface HttpHandler: (Request) -> Response {
    companion object
}

fun interface Filter : (HttpHandler) -> HttpHandler {
    companion object
}

val Filter.Companion.NoOp: Filter get() = Filter { next -> HttpHandler { next(it) } }

fun Filter.then(fn: HandleRequest) = then(HttpHandler(fn))

fun Filter.then(next: Filter) = Filter { this(next(HttpHandler(it::invoke))) }

fun Filter.then(next: HttpHandler) = HttpHandler { this(next)(it) }

fun Filter.then(routingHttpHandler: RoutingHttpHandler) = routingHttpHandler.withFilter(this)
