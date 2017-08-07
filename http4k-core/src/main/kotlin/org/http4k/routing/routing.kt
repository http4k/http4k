package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.UriTemplate
import org.http4k.routing.StaticRoutingHttpHandler.Companion.Handler as StaticHandler

interface Router {
    fun match(request: Request): HttpHandler?
}

data class Route(val method: Method, val template: UriTemplate, val handler: HttpHandler) : Router {
    override fun match(request: Request): HttpHandler? =
        if (template.matches(request.uri.path) && method == request.method) {
            { handler(it.withUriTemplate(template)) }
        } else null
}

private fun Request.withUriTemplate(uriTemplate: UriTemplate): Request = header("x-uri-template", uriTemplate.toString())

interface RoutingHttpHandler : Router, HttpHandler {
    fun withFilter(new: Filter): RoutingHttpHandler
    fun withBasePath(new: String): RoutingHttpHandler
}

fun routes(vararg list: RoutingHttpHandler): RoutingHttpHandler = object : RoutingHttpHandler {
    override fun invoke(p1: Request): Response = match(p1)?.invoke(p1) ?: Response(Status.NOT_FOUND)

    override fun match(request: Request): HttpHandler? = list.find { it.match(request) != null }

    override fun withFilter(new: Filter): RoutingHttpHandler = routes(*list.map { it.withFilter(new) }.toTypedArray())

    override fun withBasePath(new: String): RoutingHttpHandler = routes(*list.map { it.withBasePath(new) }.toTypedArray())
}

fun static(resourceLoader: ResourceLoader = ResourceLoader.Classpath(), vararg extraPairs: Pair<String, ContentType>): RoutingHttpHandler =
    StaticRoutingHttpHandler(StaticHandler("", resourceLoader, extraPairs.asList().toMap()))

fun Request.path(name: String): String? = uriTemplate().extract(uri.path)[name]

infix fun Pair<String, Method>.bind(action: HttpHandler): RoutingHttpHandler = GroupRoutingHttpHandler(
    GroupRoutingHttpHandler.Companion.Handler(null, Route(second, UriTemplate.from(first), action)))

infix fun String.bind(router: RoutingHttpHandler): RoutingHttpHandler = router.withBasePath(this)
