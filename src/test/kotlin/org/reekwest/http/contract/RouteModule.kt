package org.reekwest.http.contract

import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.then

class RouteModule private constructor(private val core: Core) : Module {

    constructor(path: BasePath, renderer: ModuleRenderer = NoRenderer, filter: Filter = Filter { it })
        : this(Core(path, emptyList(), renderer, CatchContractBreach.then(filter)))

    override fun toRouter(): Router = {
        core.routes.fold<ServerRoute, HttpHandler?>(null, { memo, serverRoute ->
            val validator = core.filter.then(ValidationFilter(serverRoute.pathBuilder.route))
            memo ?: serverRoute.match(core.rootPath)(it.method, BasePath(it.uri.path))?.let { validator.then(it) }
        })
    }

    fun withRoute(new: ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<ServerRoute>) = RouteModule(core.withRoutes(new.toList()))

    companion object {
        private class ValidationFilter(private val route: Route) : Filter {
            private fun validate(request: Request): List<ExtractionFailure> =
                route.fold(emptyList<ExtractionFailure>()) { memo, next ->
                    try {
                        next(request)
                        memo
                    } catch (e: ContractBreach) {
                        memo.plus(e.failures)
                    }
                }

            override fun invoke(next: HttpHandler): HttpHandler = {
                validate(it).let { errors ->
                    if (errors.isEmpty()) next(it) else throw ContractBreach(errors)
                }
            }
        }

        private data class Core(val rootPath: BasePath,
                                val routes: List<ServerRoute>,
                                val renderer: ModuleRenderer,
                                val filter: Filter) {
            fun withRoutes(new: List<ServerRoute>) = copy(routes = routes + new)
        }
    }
}