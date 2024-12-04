package org.http4k.routing

import org.http4k.core.Request

interface RouteMatcher<R, F> {
    fun match(request: Request): RoutingMatch<R>
    fun withBasePath(prefix: String): RouteMatcher<R, F>
    fun withRouter(other: Router): RouteMatcher<R, F>
    fun withFilter(new: F): RouteMatcher<R, F>
}

data class RoutingMatch<R>(private val priority: Int,
                           private val description: RouterDescription,
                           private val handler: (Request) -> R) :
    Comparable<RoutingMatch<R>>, (Request) -> R by handler {
    override fun compareTo(other: RoutingMatch<R>) = priority.compareTo(other.priority)
}
