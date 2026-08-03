package org.http4k.ai.mcp.server.http

import org.http4k.ai.mcp.server.asHttp
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.lens.ALLOW
import org.http4k.lens.Header
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.asRouter
import org.http4k.routing.bind
import org.http4k.routing.orElse
import org.http4k.routing.routes

fun NonStreamingMcpConnection(
    mcpProtocol: McpProtocol, path: String = "/mcp"
): RoutingHttpHandler = path bind routes(
    POST.asRouter() bind { req -> mcpProtocol.receive(req).asHttp(mcpProtocol.serverInfo) },
    orElse bind { Response.Companion(Status.METHOD_NOT_ALLOWED).with(Header.ALLOW of listOf(POST)) }
)
