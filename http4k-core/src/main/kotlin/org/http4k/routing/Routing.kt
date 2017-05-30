package org.http4k.routing

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.core.UriTemplate.Companion.from
import org.http4k.core.findSingle
import org.http4k.core.then

data class Route(val method: Method, val template: UriTemplate, val handler: HttpHandler)

fun routes(vararg routes: Route): RoutingHttpHandler = RoutingHttpHandler(routes.asList())

fun routes(module: Router, vararg then: Router): HttpHandler = then.fold(module) { memo, next -> memo.then(next) }.toHttpHandler()

fun Request.path(name: String): String? = uriTemplate().extract(uri.toString())[name]

infix fun Pair<Method, String>.by(action: HttpHandler): Route = Route(first, from(second), action)

infix fun String.by(router: RoutingHttpHandler): Router = RoutingHttpHandler(
    router.routes.map { it.copy(template = it.template.prefixedWith(this@by)) })


data class RoutingHttpHandler(internal val routes: List<Route>, internal val filter: Filter? = null) : Router, HttpHandler {
    private val routers = routes.map(Route::asRouter)
    private val noMatch: HttpHandler? = null

    override fun invoke(request: Request) = match(request)
        ?.let { handler -> (filter?.then(handler) ?: handler).invoke(request) }
        ?: Response(NOT_FOUND.description("Route not found"))

    override fun match(request: Request): HttpHandler? = routers.fold(noMatch, { memo, router -> memo ?: router.match(request) })
}

private fun Route.asRouter(): Router = object : Router {
    override fun match(request: Request): HttpHandler? =
        if (template.matches(request.uri.path) && method == request.method) {
            { req: Request -> handler(req.withUriTemplate(template)) }
        } else null
}

private fun Request.withUriTemplate(uriTemplate: UriTemplate): Request = header("x-uri-template", uriTemplate.toString())

private fun Request.uriTemplate(): UriTemplate = headers.findSingle("x-uri-template")?.let { UriTemplate.from(it) } ?: throw IllegalStateException("x-uri-template header not present in the request")