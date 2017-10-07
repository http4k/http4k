package org.http4k.contract

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.lens.Header
import org.http4k.routing.RoutingHttpHandler

data class ContractRoutingHttpHandler(private val renderer: ContractRenderer,
                                      private val security: Security,
                                      private val descriptionPath: String,
                                      private val rootAsString: String = "",
                                      private val routes: List<ContractRoute> = emptyList(),
                                      private val filter: Filter = Filter { next -> { next(it) } }
) : RoutingHttpHandler {
    private val contractRoot = PathSegments(rootAsString)
    override fun withFilter(new: Filter) = copy(filter = filter.then(new))
    override fun withBasePath(new: String) = copy(rootAsString = new + rootAsString)

    private val handler: HttpHandler = { match(it)?.invoke(it) ?: Response(NOT_FOUND.description("Route not found")) }

    override fun invoke(request: Request): Response = handler(request)

    private val descriptionRoute = ContractRouteSpec0({ PathSegments("$it$descriptionPath") }, emptyList(), null) bindContract GET to { renderer.description(contractRoot, security, routes) }

    private val routers = routes
        .map { it.toRouter(contractRoot) to CatchLensFailure.then(security.filter).then(identify(it)).then(filter) }
        .plus(descriptionRoute.toRouter(contractRoot) to identify(descriptionRoute).then(filter))

    private val noMatch: HttpHandler? = null

    override fun toString(): String = contractRoot.toString() + "\n" + routes.joinToString("\n") { it.toString() }

    override fun match(request: Request): HttpHandler? =
        if (request.isIn(contractRoot)) {
            routers.fold(noMatch, { memo, (router, routeFilter) ->
                memo ?: router.match(request)?.let { routeFilter.then(it) }
            })
        } else null

    private fun identify(route: ContractRoute): Filter =
        route.describeFor(contractRoot).let { routeIdentity ->
            Filter { next -> { next(it.with(Header.X_URI_TEMPLATE of if (routeIdentity.isEmpty()) "/" else routeIdentity)) } }
        }
}
