package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.core.UriTemplate.Companion.from
import org.http4k.routing.GroupRoutingHttpHandler.Companion.Handler
import org.http4k.routing.StaticRoutingHttpHandler.Companion.Handler as StaticHandler

data class Route(val method: Method, val template: UriTemplate, val handler: HttpHandler)

interface Router {
    fun match(request: Request): HttpHandler?
}

interface RoutingHttpHandler : Router, HttpHandler {
    fun withFilter(new: Filter): RoutingHttpHandler
    fun withBasePath(new: String): RoutingHttpHandler
}

fun routes(vararg routes: Route): RoutingHttpHandler = GroupRoutingHttpHandler(Handler(null, routes.asList()))

fun routes(first: Router, vararg then: Router): HttpHandler = then.fold(first) { memo, next -> memo.then(next) }.let {
    fold -> { request: Request -> fold.match(request)?.invoke(request) ?: Response(NOT_FOUND) }
}

fun static(resourceLoader: ResourceLoader = ResourceLoader.Classpath(), vararg extraPairs: Pair<String, ContentType>): RoutingHttpHandler =
    StaticRoutingHttpHandler(StaticHandler("", resourceLoader, extraPairs.asList().toMap()))

fun Request.path(name: String): String? = uriTemplate().extract(uri.path)[name]

infix fun Pair<String, Method>.by(action: HttpHandler): Route = Route(second, from(first), action)

infix fun String.by(router: RoutingHttpHandler): RoutingHttpHandler = router.withBasePath(this)

