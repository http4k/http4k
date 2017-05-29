package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.core.UriTemplate.Companion.uriTemplate
import org.http4k.core.findSingle

data class Route(val method: Method, val template: UriTemplate, val handler: HttpHandler)

infix fun Pair<Method, String>.by(action: HttpHandler): Route = Route(first, uriTemplate(second), action)

fun Request.path(name: String): String? = uriTemplate().extract(uri.toString())[name]

infix fun String.by(router: RoutesRouter): Module = object : Module {
    override fun toRouter(): Router = RoutesRouter(router.routes.map { it.copy(template = it.template.prefixedWith(this@by)) })
}

private fun Request.withUriTemplate(uriTemplate: UriTemplate): Request = header("x-uri-template", uriTemplate.toString())

private fun Request.uriTemplate(): UriTemplate = headers.findSingle("x-uri-template")?.let { UriTemplate.uriTemplate(it) } ?: throw IllegalStateException("x-uri-template header not present in the request")

fun routes(vararg routes: Route): RoutesRouter = RoutesRouter(routes.asList())

fun routes(module: Module, vararg then: Module): HttpHandler = then.fold(module) { memo, next -> memo.then(next) }.toHttpHandler()

fun Route.asRouter(): Router = object : Router {
    override fun match(request: Request): HttpHandler? = if (template.matches(request.uri.path) && method == request.method) {
        { req: Request -> handler(req.withUriTemplate(template)) }
    } else null
}

data class RoutesRouter(internal val routes: List<Route>): Router, HttpHandler {
    private val routers = routes.map(Route::asRouter)
    private val noMatch: HttpHandler? = null

    override fun invoke(request: Request): Response = match(request)?.invoke(request) ?: Response(NOT_FOUND)

    override fun match(request: Request): HttpHandler? = routers.fold(noMatch, { memo, router -> memo ?: router.match(request) })
}
