package org.reekwest.kontrakt.module

import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.then
import org.reekwest.kontrakt.lens.CatchContractBreach
import org.reekwest.kontrakt.module.PathBinder.Companion.Core

class RouteModule private constructor(private val router: ModuleRouter) : Module {

    constructor(moduleRoot: BasePath, renderer: ModuleRenderer = NoRenderer, filter: Filter = Filter { it })
        : this(ModuleRouter(moduleRoot, renderer, CatchContractBreach.then(filter)))

    override fun toRouter(): Router = router

    fun withRoute(new: ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<ServerRoute>) = RouteModule(router.withRoutes(new.toList()))

    companion object {
        private data class ModuleRouter(val moduleRoot: BasePath,
                                        val renderer: ModuleRenderer,
                                        val filter: Filter,
                                        val descriptionPath: (BasePath) -> BasePath = { it },
                                        val routes: List<ServerRoute> = emptyList()) : Router {
            private val routers = routes.plus(
                PathBinder0(Core(Route("description route"), GET, descriptionPath)) bind {
                    renderer.description(moduleRoot, routes)
                }).map { it.router(moduleRoot) }

            override fun invoke(request: Request): HttpHandler? =
                if (request.isIn(moduleRoot)) {
                    routers.fold<Router, HttpHandler?>(null, { memo, route ->
                        memo ?: route(request)
                    })?.let { filter.then(it) }
                } else null

            fun withRoutes(new: List<ServerRoute>) = copy(routes = routes + new)
        }
    }
}