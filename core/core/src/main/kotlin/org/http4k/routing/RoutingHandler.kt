package org.http4k.routing

import org.http4k.core.Request

/**
 * Composite handler which can potentially service many different URL patterns. Should
 * return a specified Response if it cannot service a particular Request.
 *
 * Note that generally there should be no reason for the API user to implement this interface over and above the
 * implementations that already exist. The interface is public only because we have not found a way to hide it from
 * the API user in an API-consistent manner.
 */
abstract class RoutingHandler<R, F, Self : RouteMatcher<R, F>>(
    val routes: List<RouteMatcher<R, F>>,
    private val copy: (List<RouteMatcher<R, F>>) -> Self
) : (Request) -> R, RouteMatcher<R, F> {

    init {
        require(routes.isNotEmpty(), { "No routes added!" })
    }

    override fun invoke(request: Request) = match(request)(request)

    override fun match(request: Request) = routes.minOf { it.match(request) }

    override fun withBasePath(prefix: String) = copy(routes.map { it.withBasePath(prefix) })

    override fun withFilter(new: F) = copy(routes.map { it.withFilter(new) })

    override fun withRouter(other: Router) = copy(routes.map { it.withRouter(other) })

    override fun toString() = routes
        .map(RouteMatcher<R, F>::toString)
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .joinToString("\n")
}
