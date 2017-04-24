package org.reekwest.http.contract

import org.reekwest.http.contract.spike.ModuleRenderer
import org.reekwest.http.contract.spike.NoRenderer
import org.reekwest.http.contract.spike.ServerRoute
import org.reekwest.http.contract.spike.p2.APath
import org.reekwest.http.core.*
import org.reekwest.http.core.Status.Companion.BAD_REQUEST
import org.reekwest.http.core.Status.Companion.NOT_FOUND

typealias HandlerMatcher = (Request) -> HttpHandler?

interface Module {
    infix fun then(that: Module): Module {
        val thisBinding = toHandlerMatcher()
        val thatBinding = that.toHandlerMatcher()

        return object : Module {
            override fun toHandlerMatcher(): HandlerMatcher = { req -> thisBinding(req) ?: thatBinding(req) }
        }
    }

    fun toHttpHandler(): HttpHandler {
        val svcBinding = toHandlerMatcher()
        return { req ->
            svcBinding(req)?.let {
                try {
                    it(req)
                } catch (e: ContractBreach) {
                    Response(BAD_REQUEST)
                }
            } ?: Response(NOT_FOUND)
        }
    }

    fun toHandlerMatcher(): HandlerMatcher
}


data class RouteModule(private val rootPath: APath,
                       private val routes: Iterable<ServerRoute>,
                       private val renderer: ModuleRenderer,
                       private val filter: Filter) : Module {

    constructor(path: APath, renderer: ModuleRenderer = NoRenderer) : this(path, emptyList(), renderer, Filter { it })

    private fun validate(route: ServerRoute) = Filter {
        // DO ROUTE VALIDATION HERE
        it
    }

    override fun toHandlerMatcher(): HandlerMatcher = {
        routes.fold<ServerRoute, HttpHandler?>(null, { memo, route ->
            memo ?:
                route.match(filter, rootPath)(it.method, APath(it.uri.path))?.
                    let { validate(route).then(it) }
        })
    }

    fun withRoute(new: ServerRoute) = copy(routes = routes + new)
    fun withRoutes(vararg new: ServerRoute) = copy(routes = routes + new)
    fun withRoutes(new: Iterable<ServerRoute>) = copy(routes = routes + new)
}
