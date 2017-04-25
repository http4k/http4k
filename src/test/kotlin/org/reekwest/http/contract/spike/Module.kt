package org.reekwest.http.contract.spike

import org.reekwest.http.contract.CatchContractBreach
import org.reekwest.http.contract.ContractBreach
import org.reekwest.http.contract.ExtractionFailure
import org.reekwest.http.core.*
import org.reekwest.http.core.Status.Companion.NOT_FOUND

typealias Router<T> = (T) -> HttpHandler?
typealias RequestRouter = Router<Request>

interface Module {
    infix fun then(that: Module): Module {
        val thisBinding = toRequestRouter()
        val thatBinding = that.toRequestRouter()

        return object : Module {
            override fun toRequestRouter(): RequestRouter = { req -> thisBinding(req) ?: thatBinding(req) }
        }
    }

    fun toHttpHandler(): HttpHandler {
        val svcBinding = toRequestRouter()
        return { req ->
            svcBinding(req)?.let { it(req) } ?: Response(NOT_FOUND)
        }
    }

    fun toRequestRouter(): RequestRouter
}

private class ValidationFilter(private val route: ServerRoute) : Filter {
    private fun validate(request: Request) {
        val errors = route.pathBuilder.route.fold(emptyList<ExtractionFailure>()) { memo, next ->
            try {
                next(request)
                memo
            } catch (e: ContractBreach) {
                memo.plus(e.failures)
            }
        }
        if (!errors.isEmpty()) throw ContractBreach(errors)
    }

    override fun invoke(next: HttpHandler): HttpHandler = {
        validate(it)
        next(it)
    }
}

data class RouteModule(private val rootPath: PathBuilder,
                       private val routes: Iterable<ServerRoute>,
                       private val renderer: ModuleRenderer,
                       private val filter: Filter) : Module {

    constructor(path: PathBuilder, renderer: ModuleRenderer = NoRenderer, filter: Filter = Filter { it })
        : this(path, emptyList(), renderer, CatchContractBreach.then(filter))


    override fun toRequestRouter(): RequestRouter = {
        routes.fold<ServerRoute, HttpHandler?>(null, { memo, route ->
            val validator = ValidationFilter(route).then(filter)
            memo ?: route.match(rootPath)(it.method, PathBuilder(it.uri.path))?.let { validator.then(it) }
        })
    }

    fun withRoute(new: ServerRoute) = copy(routes = routes + new)
    fun withRoutes(vararg new: ServerRoute) = copy(routes = routes + new)
    fun withRoutes(new: Iterable<ServerRoute>) = copy(routes = routes + new)
}
