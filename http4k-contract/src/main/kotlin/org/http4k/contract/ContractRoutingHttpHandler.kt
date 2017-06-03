package org.http4k.contract

import org.http4k.contract.PathBinder.Companion.Core
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header.X_URI_TEMPLATE
import org.http4k.routing.RoutingHttpHandler

class ContractRoutingHttpHandler internal constructor(val httpHandler: ContractRoutingHttpHandler.Companion.Handler) : RoutingHttpHandler {
    override fun match(request: Request): HttpHandler? = httpHandler.match(request)

    override fun invoke(request: Request): Response = httpHandler(request)

    override fun withBasePath(new: String): ContractRoutingHttpHandler = ContractRoutingHttpHandler(httpHandler.withBasePath(new))
    override fun withFilter(new: Filter): RoutingHttpHandler = ContractRoutingHttpHandler(httpHandler.withFilter(new))
    fun withRoute(new: ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<ServerRoute>) = ContractRoutingHttpHandler(httpHandler.withRoutes(new))

    companion object {
        internal data class Handler(private val renderer: ContractRenderer,
                                    private val security: Security,
                                    private val descriptionPath: String,
                                    private val rootAsString: String = "",
                                    private val routes: List<ServerRoute> = emptyList(),
                                    private val filter: Filter = ServerFilters.CatchLensFailure
                                    ) : HttpHandler {
            private val contractRoot = BasePath(rootAsString)

            internal fun withRoutes(new: Iterable<ServerRoute>) = copy(routes = routes + new)
            internal fun withFilter(new: Filter) = copy(filter = filter.then(new))
            internal fun withBasePath(new: String) = copy(rootAsString = new + rootAsString)

            private val handler: HttpHandler = { match(it)?.invoke(it) ?: Response(NOT_FOUND.description("Route not found")) }

            override fun invoke(request: Request): Response = handler(request)

            private val descriptionRoute = PathBinder0(Core(Route("description route"), GET, { BasePath("$it$descriptionPath") })) bind
                { renderer.description(contractRoot, security, routes) }

            private val routers = routes
                .map { it.router(contractRoot) to security.filter.then(identify(it)).then(filter) }
                .plus(descriptionRoute.router(contractRoot) to identify(descriptionRoute).then(filter))

            private val noMatch: HttpHandler? = null

            fun match(request: Request): HttpHandler? =
                if (request.isIn(contractRoot)) {
                    routers.fold(noMatch, { memo, (router, routeFilter) ->
                        memo ?: router.match(request)?.let { routeFilter.then(it) }
                    })
                } else null

            private fun identify(route: ServerRoute): Filter {
                val routeIdentity = route.describeFor(contractRoot)
                return Filter {
                    { req ->
                        it(req.with(X_URI_TEMPLATE of if (routeIdentity.isEmpty()) "/" else routeIdentity))
                    }
                }
            }

        }
    }

}