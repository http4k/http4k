package org.http4k.wiretap.mcp.apps

import org.http4k.ai.mcp.apps.model.AvailableMcpApp
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json

fun Index(html: TemplateRenderer, mcpApps: List<AvailableMcpApp>): RoutingHttpHandler =
    "/" bind GET to { Response(OK).html(html(Index(mcpApps))) }

data class Index(val tools: List<AvailableMcpApp>) : ViewModel {
    val initialSignals: String = Json.asFormatString(McpAppSignals())
}

data class McpAppSignals(val selectedServerId: String = "", val iframeVisible: Boolean = false)
