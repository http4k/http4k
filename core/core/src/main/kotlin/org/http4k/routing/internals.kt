package org.http4k.routing

import org.http4k.core.Request

interface RouteMatcher<R, F> {
    fun match(request: Request): RoutingMatchResult<R>
    fun withBasePath(prefix: String): RouteMatcher<R, F>
    fun withRouter(other: Router): RouteMatcher<R, F>
    fun withFilter(new: F): RouteMatcher<R, F>
}

data class RoutingMatchResult<R>(val priority: Int, val handler: (Request) -> R)
