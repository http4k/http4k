package org.http4k.wiretap.mcp.tools

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json

data class McpToolView(val name: String, val description: String, val inputSchema: String)

fun McpTool.toView() = McpToolView(
    name = name.value,
    description = description,
    inputSchema = Json.asFormatString(inputSchema)
)

data class TabContent(val tools: List<McpToolView>) : ViewModel

fun ToolsTabContent(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/tab/tools" bind GET to {
        val tools = mcpClient.tools().list()
            .map { list -> list.map { tool -> tool.toView() } }
            .valueOrNull() ?: emptyList()

        Response(OK).datastarElements(
            elements(TabContent(tools)),
            selector = Selector.of("#mcp-content")
        )
    }
