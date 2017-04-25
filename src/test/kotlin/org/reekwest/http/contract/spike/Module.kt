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
        val thisRouter = toRequestRouter()
        val thatRouter = that.toRequestRouter()

        return object : Module {
            override fun toRequestRouter(): RequestRouter = { req -> thisRouter(req) ?: thatRouter(req) }
        }
    }

    fun toHttpHandler(): HttpHandler {
        val router = toRequestRouter()
        return { req ->
            router(req)?.let { it(req) } ?: Response(NOT_FOUND)
        }
    }

    fun toRequestRouter(): RequestRouter
}

private class ValidationFilter(private val serverRoute: ServerRoute) : Filter {
    private fun validate(request: Request): List<ExtractionFailure> =
        serverRoute.pathBuilder.route.fold(emptyList<ExtractionFailure>()) { memo, next ->
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

class RouteModule private constructor(private val core: ModuleCore) : Module {

    constructor(path: Path, renderer: ModuleRenderer = NoRenderer, filter: Filter = Filter { it })
        : this(ModuleCore(path, emptyList(), renderer, CatchContractBreach.then(filter)))

    override fun toRequestRouter(): RequestRouter = {
        core.routes.fold<ServerRoute, HttpHandler?>(null, { memo, route ->
            val validator = core.filter.then(ValidationFilter(route))
            memo ?: route.match(core.rootPath)(it.method, Path(it.uri.path))?.let { validator.then(it) }
        })
    }

    fun withRoute(new: ServerRoute) = withRoutes(new)
    fun withRoutes(vararg new: ServerRoute) = withRoutes(new.toList())
    fun withRoutes(new: Iterable<ServerRoute>) = RouteModule(core.withRoutes(new.toList()))

    companion object {
        private data class ModuleCore(val rootPath: Path,
                                      val routes: List<ServerRoute>,
                                      val renderer: ModuleRenderer,
                                      val filter: Filter) {
            fun withRoutes(new: List<ServerRoute>) = copy(routes = routes + new)
        }
    }
}
