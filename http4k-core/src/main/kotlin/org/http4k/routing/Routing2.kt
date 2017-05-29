package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Request

infix fun String.by(router: RoutesRouter): Module = object: Module {
    override fun toRouter(): Router = RoutesRouter(router.routes.map { it.copy(template = it.template.prefixedWith(this@by)) })
}

fun routes(module: Module, vararg then: Module): HttpHandler = then.fold(module) { memo, next -> memo.then(next) }.toHttpHandler()

fun Route.asRouter(): Router = { request -> if (template.matches(request.uri.path) && method == request.method) handler else null }

data class RoutesRouter(internal val routes: List<Route>) : Router {
    private val routers = routes.map(Route::asRouter)
    private val noMatch: HttpHandler? = null
    override fun invoke(request: Request): HttpHandler? = routers.fold(noMatch, { memo, router -> memo ?: router(request) })
}

fun routesMk2(vararg routes: Route): RoutesRouter = RoutesRouter(routes.asList())
