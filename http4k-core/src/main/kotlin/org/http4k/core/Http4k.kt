package org.http4k.core

import org.http4k.routing.GroupRoutingHttpHandler
import org.http4k.routing.RoutingHttpHandler

typealias HttpHandler = (Request) -> Response

interface Filter : (HttpHandler) -> HttpHandler {
    companion object {
        operator fun invoke(fn: (HttpHandler) -> HttpHandler): Filter = object : Filter {
            operator override fun invoke(next: HttpHandler): HttpHandler = fn(next)
        }
    }
}

fun Filter.then(router: GroupRoutingHttpHandler): GroupRoutingHttpHandler = router.copy(filter = this)

fun Filter.then(next: Filter): Filter = Filter { this(next(it)) }

fun Filter.then(next: HttpHandler): HttpHandler = { this(next)(it) }

fun Filter.then(router: RoutingHttpHandler): RoutingHttpHandler = this.then(router.toHttpHandler()).let {
    routerAsHandler ->
    object : RoutingHttpHandler {
        override fun invoke(request: Request): Response = routerAsHandler(request)
        override fun match(request: Request): HttpHandler? = router.match(request)?.let { this@then.then(routerAsHandler) }
    }
}
