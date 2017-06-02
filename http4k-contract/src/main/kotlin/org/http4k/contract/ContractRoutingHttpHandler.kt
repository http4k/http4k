package org.http4k.contract

import org.http4k.contract.PathBinder.Companion.Core
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.Header.X_URI_TEMPLATE
import org.http4k.routing.Router
import org.http4k.routing.RoutingHttpHandler

class ContractRoutingHttpHandler internal constructor(val httpHandler: ContractRoutingHttpHandler.Companion.Handler) : RoutingHttpHandler {
    override fun match(request: Request): HttpHandler? = httpHandler.match(request)

    override fun invoke(request: Request): Response = httpHandler(request)

    override fun withBasePath(basePath: String): ContractRoutingHttpHandler = ContractRoutingHttpHandler(httpHandler.copy(rootAsString = basePath + httpHandler.rootAsString))
    override fun withFilter(filter: Filter): RoutingHttpHandler = ContractRoutingHttpHandler(httpHandler.copy(filter = httpHandler.filter.then(filter)))
    fun withRoute(new: ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<ServerRoute>) = ContractRoutingHttpHandler(httpHandler.copy(routes = httpHandler.routes + new))

    companion object {
        internal data class Handler(internal val rootAsString: String,
                                    private val renderer: ContractRenderer,
                                    internal val filter: Filter,
                                    private val security: Security = NoSecurity,
                                    private val descriptionPath: String = "",
                                    internal val routes: List<ServerRoute>) : HttpHandler {
            private val contractRoot = BasePath(rootAsString)

            private val handler: HttpHandler = { match(it)?.invoke(it) ?: Response(Status.NOT_FOUND.description("Route not found")) }

            override fun invoke(request: Request): Response = handler(request)

            private val descriptionRoute = PathBinder0(Core(Route("description route"), GET, { BasePath("$it$descriptionPath") })) bind
                { renderer.description(contractRoot, security, routes) }

            private val routers: List<Pair<Router, Filter>> = routes
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