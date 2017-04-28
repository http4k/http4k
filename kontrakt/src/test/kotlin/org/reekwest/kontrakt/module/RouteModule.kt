package org.reekwest.kontrakt.module

import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.then
import org.reekwest.kontrakt.CatchContractBreach

class RouteModule private constructor(private val core: ModuleRouter) : Module {

    constructor(path: BasePath, renderer: ModuleRenderer = NoRenderer, filter: Filter = Filter { it })
        : this(ModuleRouter(path, renderer, CatchContractBreach.then(filter)))

    override fun toRouter(): Router = core

    fun withRoute(new: ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<ServerRoute>) = RouteModule(core.withRoutes(new.toList()))

    companion object {
        private data class ModuleRouter(val moduleRoot: BasePath,
                                        val renderer: ModuleRenderer,
                                        val filter: Filter,
                                        val routes: List<ServerRoute> = emptyList()) : Router {
            override fun invoke(request: Request): HttpHandler? =
                routes.fold<ServerRoute, HttpHandler?>(null, { memo, serverRoute ->
                    val validator = filter.then(serverRoute.pathBinder.core.route.validationFilter())
                    memo ?: serverRoute.router(moduleRoot)(request)?.let { validator.then(it) }
                })

            fun withRoutes(new: List<ServerRoute>) = copy(routes = routes + new)
        }
    }
}