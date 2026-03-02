package org.http4k.wiretap.mcp.client.tools

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json

data class McpClientToolsSignals(val selectedItem: String = "")

data class McpToolView(val name: String, val description: String, val inputSchema: String)

fun Index(mcpClient: McpClient, html: TemplateRenderer) = "/" bind GET to {
    val tools = mcpClient.tools().list()
        .map { list -> list.map { tool -> tool.toView() } }
        .valueOrNull() ?: emptyList()

    Response(OK).html(html(Index(tools)))
}

data class Index(val tools: List<McpToolView>) : ViewModel {
    val initialSignals: String = Json.asFormatString(McpClientToolsSignals())
}

private fun McpTool.toView() = McpToolView(
    name = name.value,
    description = description,
    inputSchema = Json.asFormatString(inputSchema)
)
