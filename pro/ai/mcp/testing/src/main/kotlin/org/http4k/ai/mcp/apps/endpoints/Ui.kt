package org.http4k.ai.mcp.apps.endpoints

import org.http4k.ai.mcp.apps.McpApps
import org.http4k.ai.mcp.apps.model.ToolOption
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel

fun Ui(servers: McpApps): RoutingHttpHandler {
    val renderer = HandlebarsTemplates().CachingClasspath()
    return "/" bind GET to { Response(OK).body(renderer(Ui(servers.tools()))) }
}

data class Ui(val tools: List<ToolOption>) : ViewModel
