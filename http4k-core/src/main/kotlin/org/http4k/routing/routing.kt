package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.UriTemplate

fun routes(vararg list: Pair<Method, HttpHandler>): RoutingHttpHandler =
    routes(*list.map { "" bind it.first to it.second }.toTypedArray())

fun routes(vararg list: RoutingHttpHandler): RoutingHttpHandler = RouterBasedHttpHandler(OrRouter.from(list.toList()))

infix fun String.bind(method: Method): PathMethod = PathMethod(this, method)
infix fun String.bind(httpHandler: RoutingHttpHandler): RoutingHttpHandler = httpHandler.withBasePath(this)
infix fun String.bind(action: HttpHandler): RoutingHttpHandler =
    RouterBasedHttpHandler(TemplateRouter(UriTemplate.from(this), action))

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler =
    RouterBasedHttpHandler(and(PassthroughRouter(handler)))

infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = RouterBasedHttpHandler(and(handler))
infix fun Router.and(that: Router): Router = AndRouter.from(listOf(this, that))

/**
 * Simple Reverse Proxy which will split and direct traffic to the appropriate
 * HttpHandler based on the content of the Host header
 */
fun reverseProxy(vararg hostToHandler: Pair<String, HttpHandler>): HttpHandler = reverseProxyRouting(*hostToHandler)

/**
 * Simple Reverse Proxy. Exposes routing.
 */
fun reverseProxyRouting(vararg hostToHandler: Pair<String, HttpHandler>) = routes(
    *hostToHandler
        .map { service ->
            hostHeaderOrUri { it.contains(service.first) } bind service.second
        }.toTypedArray()
)

private fun hostHeaderOrUri(fn: (String) -> Boolean) =
    { req: Request ->
        (req.headerValues("host").firstOrNull() ?: req.uri.authority).let(fn)
    }.asRouter("Host header matching $fn")

/**
 * Apply routing predicate to a query
 */
fun query(name: String, fn: (String) -> Boolean) =
    { req: Request -> req.queries(name).filterNotNull().any(fn) }.asRouter("Query $name matching $fn")

/**
 * Apply routing predicate to a query
 */
fun query(name: String, value: String) = query(name) { it == value }

/**
 * Ensure all queries are present
 */
fun queries(vararg names: String) = { req: Request -> names.all { req.query(it) != null } }.asRouter("Queries ${names.toList()}")

/**
 * Apply routing predicate to a header
 */
fun header(name: String, fn: (String) -> Boolean) =
    { req: Request -> req.headerValues(name).filterNotNull().any(fn) }.asRouter("Header $name matching $fn")

/**
 * Apply routing predicate to a header
 */
fun header(name: String, value: String) = header(name) { it == value }

/**
 * Ensure all headers are present
 */
fun headers(vararg names: String) = { req: Request -> names.all { req.header(it) != null } }.asRouter("Headers ${names.toList()}")

/**
 * Ensure body matches predicate
 */
fun body(fn: (Body) -> Boolean) = { req: Request -> fn(req.body) }.asRouter("Body matching $fn")

/**
 * Ensure body string matches predicate
 */
@JvmName("bodyMatches")
fun body(fn: (String) -> Boolean) = { req: Request -> fn(req.bodyString()) }.asRouter("Body matching $fn")
