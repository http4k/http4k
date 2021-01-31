package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.sse.SseConsumer
import org.http4k.websocket.WsConsumer

fun routes(vararg list: Pair<Method, HttpHandler>): RoutingHttpHandler = routes(*list.map { "" bind it.first to it.second }.toTypedArray())
fun routes(vararg list: RoutingHttpHandler): RoutingHttpHandler = RouterBasedHttpHandler(OrRouter.from(list.toList()))

infix fun String.bind(method: Method): PathMethod = PathMethod(this, method)
infix fun String.bind(httpHandler: RoutingHttpHandler): RoutingHttpHandler = httpHandler.withBasePath(this)
infix fun String.bind(action: HttpHandler): RoutingHttpHandler = RouterBasedHttpHandler(TemplateRouter(UriTemplate.from(this), action))

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler = RouterBasedHttpHandler(and(PassthroughRouter(handler)))
infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = RouterBasedHttpHandler(and(handler))
infix fun Router.and(that: Router): Router = AndRouter.from(listOf(this, that))

infix fun String.bind(consumer: WsConsumer): RoutingWsHandler = TemplateRoutingWsHandler(UriTemplate.from(this), consumer)
infix fun String.bind(wsHandler: RoutingWsHandler): RoutingWsHandler = wsHandler.withBasePath(this)

infix fun String.bind(consumer: SseConsumer): RoutingSseHandler = TemplateRoutingSseHandler(UriTemplate.from(this), consumer)
infix fun String.bind(sseHandler: RoutingSseHandler): RoutingSseHandler = sseHandler.withBasePath(this)

/**
 * Simple Reverse Proxy which will split and direct traffic to the appropriate
 * HttpHandler based on the content of the Host header
 */
fun reverseProxy(vararg hostToHandler: Pair<String, HttpHandler>) = routes(
    *hostToHandler
        .map { service ->
            header("host") { it.contains(service.first) } bind service.second
        }.toTypedArray()
)

/**
 * Apply routing predicate to a query
 */
fun query(name: String, predicate: (String) -> Boolean) = { req: Request -> req.queries(name).filterNotNull().any(predicate) }.asRouter()

/**
 * Apply routing predicate to a query
 */
fun query(name: String, value: String) = query(name) { it == value }

/**
 * Ensure all queries are present
 */
fun queries(vararg names: String) = { req: Request -> names.all { req.query(it) != null } }.asRouter()

/**
 * Apply routing predicate to a header
 */
fun header(name: String, predicate: (String) -> Boolean) = { req: Request -> req.headerValues(name).filterNotNull().any(predicate) }.asRouter()

/**
 * Apply routing predicate to a header
 */
fun header(name: String, value: String) = header(name) { it == value }

/**
 * Ensure all headers are present
 */
fun headers(vararg names: String) = { req: Request -> names.all { req.header(it) != null } }.asRouter()

/**
 * Ensure body matches predicate
 */
fun body(predicate: (Body) -> Boolean) = { req: Request -> predicate(req.body) }.asRouter()

/**
 * Ensure body string matches predicate
 */
@JvmName("bodyMatches")
fun body(predicate: (String) -> Boolean) = { req: Request -> predicate(req.bodyString()) }.asRouter()
