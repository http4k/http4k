package org.http4k.routing

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.UriTemplate
import org.http4k.websocket.WsConsumer

fun routes(vararg list: Pair<Method, HttpHandler>): RoutingHttpHandler = routes(*list.map { "" bind it.first to it.second }.toTypedArray())

fun routes(vararg list: RoutingHttpHandler): RoutingHttpHandler = RouterRoutingHttpHandler(OrRouter(list.toList()))

infix fun String.bind(method: Method): PathMethod = PathMethod(this, method)

infix fun String.bind(httpHandler: RoutingHttpHandler): RoutingHttpHandler = httpHandler.withBasePath(this)

infix fun String.bind(action: HttpHandler): RoutingHttpHandler = RouterRoutingHttpHandler(TemplatingRouter(null, UriTemplate.from(this), action))

infix fun String.bind(consumer: WsConsumer): RoutingWsHandler = TemplateRoutingWsHandler(UriTemplate.from(this), consumer)

infix fun String.bind(wsHandler: RoutingWsHandler): RoutingWsHandler = wsHandler.withBasePath(this)

/**
 * For routes where certain queries are required for correct operation. Router is composable.
 */
fun queries(vararg names: String) = { req: Request -> names.all { req.query(it) != null } }.asRouter()

/**
 * For routes where certain headers are required for correct operation. Router is composable.
 */
fun headers(vararg names: String) = { req: Request -> names.all { req.header(it) != null } }.asRouter()

/**
 * Matches the Host header to a matching Handler.
 */
fun hostDemux(vararg hosts: Pair<String, RoutingHttpHandler>) =
    routes(*hosts.map { { req: Request -> (req.header("host") == it.first) }.asRouter() bind it.second }.toTypedArray())

infix fun Router.bind(handler: HttpHandler): RoutingHttpHandler = RouterRoutingHttpHandler(and(PassthroughRouter(handler)))
infix fun Router.bind(handler: RoutingHttpHandler): RoutingHttpHandler = RouterRoutingHttpHandler(and(handler))
infix fun Router.and(that: Router): Router = AndRouter(listOf(this, that))
