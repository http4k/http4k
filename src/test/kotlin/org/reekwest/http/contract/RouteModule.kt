package org.reekwest.http.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.then

typealias Router<T> = (T) -> org.reekwest.http.core.HttpHandler?
typealias RequestRouter = org.reekwest.http.contract.Router<Request>

class RouteModule private constructor(private val core: org.reekwest.http.contract.RouteModule.Companion.Core) : org.reekwest.http.contract.Module {

    constructor(path: BasePath, renderer: ModuleRenderer = NoRenderer, filter: org.reekwest.http.core.Filter = org.reekwest.http.core.Filter { it })
        : this(org.reekwest.http.contract.RouteModule.Companion.Core(path, emptyList(), renderer, CatchContractBreach.then(filter)))

    override fun toRequestRouter(): org.reekwest.http.contract.RequestRouter = {
        core.routes.fold<org.reekwest.http.contract.ServerRoute, org.reekwest.http.core.HttpHandler?>(null, { memo, serverRoute ->
            val validator = core.filter.then(org.reekwest.http.contract.RouteModule.Companion.ValidationFilter(serverRoute.pathBuilder.route))
            memo ?: serverRoute.match(core.rootPath)(it.method, BasePath(it.uri.path))?.let { validator.then(it) }
        })
    }

    fun withRoute(new: org.reekwest.http.contract.ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: org.reekwest.http.contract.ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<org.reekwest.http.contract.ServerRoute>) = org.reekwest.http.contract.RouteModule(core.withRoutes(new.toList()))

    companion object {
        private class ValidationFilter(private val route: org.reekwest.http.contract.Route) : org.reekwest.http.core.Filter {
            private fun validate(request: org.reekwest.http.core.Request): List<org.reekwest.http.contract.ExtractionFailure> =
                route.fold(emptyList<org.reekwest.http.contract.ExtractionFailure>()) { memo, next ->
                    try {
                        next(request)
                        memo
                    } catch (e: org.reekwest.http.contract.ContractBreach) {
                        memo.plus(e.failures)
                    }
                }

            override fun invoke(next: org.reekwest.http.core.HttpHandler): org.reekwest.http.core.HttpHandler = {
                validate(it).let { errors ->
                    if (errors.isEmpty()) next(it) else throw org.reekwest.http.contract.ContractBreach(errors)
                }
            }
        }

        private data class Core(val rootPath: BasePath,
                                val routes: List<org.reekwest.http.contract.ServerRoute>,
                                val renderer: ModuleRenderer,
                                val filter: org.reekwest.http.core.Filter) {
            fun withRoutes(new: List<org.reekwest.http.contract.ServerRoute>) = copy(routes = routes + new)
        }
    }
}