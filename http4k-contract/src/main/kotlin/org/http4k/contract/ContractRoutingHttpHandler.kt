package org.http4k.contract

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.routing.RoutedRequest
import org.http4k.routing.RoutedResponse
import org.http4k.routing.Router
import org.http4k.routing.RoutingHttpHandler

data class ContractRoutingHttpHandler(private val renderer: ContractRenderer,
                                      private val security: Security,
                                      private val descriptionPath: String,
                                      private val routes: List<ContractRoute> = emptyList(),
                                      private val rootAsString: String = "",
                                      private val preSecurityFilter: Filter = Filter.NoOp,
                                      private val postSecurityFilter: Filter = Filter.NoOp
) : RoutingHttpHandler {
    private val contractRoot = PathSegments(rootAsString)

    fun withPostSecurityFilter(new: Filter) = copy(postSecurityFilter = postSecurityFilter.then(new))

    /**
     * NOTE: By default, filters for Contracts are applied *after* the Security filter. Use withPreSecurityFilter()
     * to achieve population of filters before security.
     */
    override fun withFilter(new: Filter) = copy(preSecurityFilter = preSecurityFilter.then(new))

    override fun withBasePath(new: String) = copy(rootAsString = new + rootAsString)

    private val handler: HttpHandler = { match(it)?.invoke(it) ?: Response(NOT_FOUND.description("Route not found")) }

    override fun invoke(request: Request): Response = handler(request)

    private val descriptionRoute = ContractRouteSpec0({ PathSegments("$it$descriptionPath") }, RouteMeta()) bindContract GET to { renderer.description(contractRoot, security, routes) }

    private val routers: List<Pair<Filter, Router>> = routes
        .map { CatchLensFailure.then(identify(it)).then(preSecurityFilter).then(security.filter).then(postSecurityFilter) to it.toRouter(contractRoot) }
        .plus(identify(descriptionRoute).then(postSecurityFilter) to descriptionRoute.toRouter(contractRoot))

    private val noMatch: HttpHandler? = null

    override fun toString(): String = contractRoot.toString() + "\n" + routes.joinToString("\n") { it.toString() }

    override fun match(request: Request): HttpHandler? =
        if (request.isIn(contractRoot)) {
            routers.fold(noMatch) { memo, (routeFilter, router) ->
                memo ?: router.match(request)?.let { routeFilter.then(it) }
            }
        } else null

    private fun identify(route: ContractRoute): Filter =
        route.describeFor(contractRoot).let { routeIdentity ->
            Filter { next ->
                {
                    val xUriTemplate = UriTemplate.from(if (routeIdentity.isEmpty()) "/" else routeIdentity)
                    RoutedResponse(next(RoutedRequest(it, xUriTemplate)), xUriTemplate)
                }
            }
        }
}
