package org.http4k.routing

import org.http4k.core.Request

/**
 * Composite andler which can potentially service many different URL patterns. Should
 * return a specified Response if it cannot service a particular Request.
 *
 * Note that generally there should be no reason for the API user to implement this interface over and above the
 * implementations that already exist. The interface is public only because we have not found a way to hide it from
 * the API user in an API-consistent manner.
 */
abstract class RoutingHandler<R, F, Self>(
    val routes: List<RouteMatcher<R, F>>,
    private val notFoundResponse: R,
    private val copy: (List<RouteMatcher<R, F>>) -> Self
) : (Request) -> R {
    override fun invoke(request: Request) = routes
        .minOfOrNull { it.match(request) }
        ?.invoke(request)
        ?: notFoundResponse

    fun withBasePath(prefix: String) = copy(routes.map { it.withBasePath(prefix) })

    fun withFilter(new: F) = copy(routes.map { it.withFilter(new) })

    fun withRouter(router: Router) = copy(routes.map { it.withRouter(router) })

    override fun toString() = routes.sortedBy(RouteMatcher<R, F>::toString).joinToString("\n")
}
