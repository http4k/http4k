package org.http4k.contract

import org.http4k.contract.PathBinder.Companion.Core
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header.X_URI_TEMPLATE
import org.http4k.routing.Router

class ContractRouter private constructor(private val router: ModuleRouter) : Router {
    override fun match(request: Request): HttpHandler? = router.match(request)

    constructor(moduleRoot: BasePath, renderer: ModuleRenderer = NoRenderer, filter: Filter = Filter { it })
        : this(ModuleRouter(moduleRoot, renderer, ServerFilters.CatchLensFailure.then(filter)))

    fun securedBy(new: Security) = ContractRouter(router.securedBy(new))
    fun withDescriptionPath(fn: (BasePath) -> BasePath) = ContractRouter(router.copy(descriptionPath = fn))
    fun withRoute(new: ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<ServerRoute>) = ContractRouter(router.withRoutes(new.toList()))

    companion object {
        private data class ModuleRouter(val moduleRoot: BasePath,
                                        val renderer: ModuleRenderer,
                                        val filter: Filter,
                                        val security: Security = NoSecurity,
                                        val descriptionPath: (BasePath) -> BasePath = { it },
                                        val routes: List<ServerRoute> = emptyList()) : Router {
            private val descriptionRoute = descriptionRoute()

            private val routers: List<Pair<Router, Filter>> = routes
                .map { it.router(moduleRoot) to security.filter.then(identify(it)).then(filter) }
                .plus(descriptionRoute.router(moduleRoot) to identify(descriptionRoute).then(filter))

            private val noMatch: HttpHandler? = null

            override fun match(request: Request): HttpHandler? =
                if (request.isIn(moduleRoot)) {
                    routers.fold(noMatch, { memo, (router, routeFilter) ->
                        memo ?: router.match(request)?.let { routeFilter.then(it) }
                    })
                } else null

            fun withRoutes(new: List<ServerRoute>) = copy(routes = routes + new)
            fun securedBy(new: Security) = copy(security = new)

            private fun descriptionRoute(): ServerRoute =
                PathBinder0(Core(Route("description route"), GET, descriptionPath)) bind
                    { renderer.description(moduleRoot, security, routes) }

            private fun identify(route: ServerRoute): Filter {
                val routeIdentity = route.describeFor(moduleRoot)
                return Filter {
                    { req ->
                        it(req.with(X_URI_TEMPLATE of if (routeIdentity.isEmpty()) "/" else routeIdentity))
                    }
                }
            }

        }
    }
}