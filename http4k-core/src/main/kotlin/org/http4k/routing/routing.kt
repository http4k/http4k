package org.http4k.routing

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.UriTemplate
import org.http4k.websocket.RoutingWsHandler
import org.http4k.websocket.TemplatingRoutingWsHandler
import org.http4k.websocket.WsConsumer

interface Router {
    fun match(request: Request): HttpHandler?
}

interface RoutingHttpHandler : Router, HttpHandler {
    fun withFilter(new: Filter): RoutingHttpHandler
    fun withBasePath(new: String): RoutingHttpHandler
}

fun routes(vararg list: Pair<Method, HttpHandler>): RoutingHttpHandler = routes(*list.map { "" bind it.first to it.second }.toTypedArray())

fun routes(vararg list: RoutingHttpHandler): RoutingHttpHandler = object : RoutingHttpHandler {
    override fun invoke(p1: Request): Response = match(p1)?.invoke(p1) ?: Response(NOT_FOUND.description("Route not found"))

    override fun match(request: Request): HttpHandler? = list.find { it.match(request) != null }

    override fun withFilter(new: Filter): RoutingHttpHandler = routes(*list.map { it.withFilter(new) }.toTypedArray())

    override fun withBasePath(new: String): RoutingHttpHandler = routes(*list.map { it.withBasePath(new) }.toTypedArray())
}

fun static(resourceLoader: ResourceLoader = ResourceLoader.Classpath(), vararg extraPairs: Pair<String, ContentType>): StaticRoutingHttpHandler =
    StaticRoutingHttpHandler("", resourceLoader, extraPairs.asList().toMap())

fun websockets(vararg list: RoutingWsHandler): RoutingWsHandler = object : RoutingWsHandler {
    override operator fun invoke(request: Request): WsConsumer? = list.firstOrNull { it.invoke(request) != null }?.invoke(request)
    override fun withBasePath(new: String): RoutingWsHandler = websockets(*list.map { it.withBasePath(new) }.toTypedArray())
}

fun Request.path(name: String): String? = uriTemplate().extract(uri.path)[name]

data class PathMethod(val path: String, val method: Method) {
    infix fun to(action: HttpHandler): RoutingHttpHandler = TemplateRoutingHttpHandler(method, UriTemplate.from(path), action)
    infix fun to(action: StaticRoutingHttpHandler): RoutingHttpHandler = action.withBasePath(path).let {
        object : RoutingHttpHandler by it {
            override fun match(request: Request): HttpHandler? = when (method) {
                request.method -> it.match(request)
                else -> null
            }
        }
    }
}

infix fun String.bind(method: Method): PathMethod = PathMethod(this, method)

infix fun String.bind(httpHandler: RoutingHttpHandler): RoutingHttpHandler = httpHandler.withBasePath(this)

infix fun String.bind(action: HttpHandler): RoutingHttpHandler = TemplateRoutingHttpHandler(null, UriTemplate.from(this), action)

infix fun String.bind(consumer: WsConsumer): RoutingWsHandler = TemplatingRoutingWsHandler(UriTemplate.from(this), consumer)

infix fun String.bind(wsHandler: RoutingWsHandler): RoutingWsHandler = wsHandler.withBasePath(this)