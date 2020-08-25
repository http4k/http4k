package org.http4k.core

import org.http4k.routing.RoutingHttpHandler

typealias HttpHandler = (Request) -> Response

interface Filter : (HttpHandler) -> HttpHandler {
    companion object {
        @JvmStatic
        @JvmName("create")
        operator fun invoke(fn: (HttpHandler) -> HttpHandler): Filter = object : Filter {
            override operator fun invoke(next: HttpHandler): HttpHandler = fn(next)
        }
    }
}

val Filter.Companion.NoOp: Filter get() = Filter { next -> { next(it) } }

fun Filter.then(next: Filter): Filter = Filter { this(next(it)) }

fun Filter.then(next: HttpHandler): HttpHandler = { this(next)(it) }

fun Filter.then(routingHttpHandler: RoutingHttpHandler): RoutingHttpHandler = routingHttpHandler.withFilter(this)
