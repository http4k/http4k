package org.http4k.core

import org.http4k.routing.RoutingHttpHandler

typealias HttpHandler = (Request) -> Response

interface Filter : (HttpHandler) -> HttpHandler {
    companion object {
        operator fun invoke(fn: (HttpHandler) -> HttpHandler): Filter = object : Filter {
            operator override fun invoke(next: HttpHandler): HttpHandler = fn(next)
        }
    }
}

fun Filter.then(next: Filter): Filter = Filter { this(next(it)) }

fun Filter.then(next: HttpHandler): HttpHandler = { this(next)(it) }

fun Filter.then(routingHttpHandler: RoutingHttpHandler): RoutingHttpHandler = this.then(routingHttpHandler.toHttpHandler()).let {
    routerAsHandler ->
    object : RoutingHttpHandler {
        override fun withBasePath(basePath: String): RoutingHttpHandler = routingHttpHandler.withBasePath(basePath)
        override fun invoke(request: Request): Response = routerAsHandler(request)
        override fun match(request: Request): HttpHandler? = routingHttpHandler.match(request)?.let { this@then.then(routerAsHandler) }
    }
}
