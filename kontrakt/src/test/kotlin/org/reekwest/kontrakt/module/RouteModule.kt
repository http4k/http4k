package org.reekwest.kontrakt.module

import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.then
import org.reekwest.kontrakt.CatchContractBreach

class RouteModule private constructor(private val core: Core) : Module {

    constructor(path: BasePath, renderer: ModuleRenderer = NoRenderer, filter: Filter = Filter { it })
        : this(Core(path, emptyList(), renderer, CatchContractBreach.then(filter)))

    override fun toRouter(): Router = core.router

    fun withRoute(new: ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<ServerRoute>) = RouteModule(core.withRoutes(new.toList()))

    companion object {

        private data class Core(val rootPath: BasePath,
                                val routes: List<ServerRoute>,
                                val renderer: ModuleRenderer,
                                val filter: Filter) {
            fun withRoutes(new: List<ServerRoute>) = copy(routes = routes + new)

            val router: Router = {
                routes.fold<ServerRoute, HttpHandler?>(null, { memo, serverRoute ->
                    val validator = filter.then(serverRoute.pathBinder.core.route.validationFilter())
                    memo ?: serverRoute.router(rootPath)(it)?.let { validator.then(it) }
                })
            }
        }
    }
}