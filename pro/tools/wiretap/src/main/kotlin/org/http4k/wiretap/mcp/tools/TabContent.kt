package org.http4k.wiretap.mcp.tools

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.core.Method.GET
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.mcp.McpListItem
import org.http4k.wiretap.mcp.McpListTabContent
import org.http4k.wiretap.mcp.McpTabResetSignals
import org.http4k.wiretap.mcp.mcpTabResponse
import org.http4k.wiretap.util.Json

data class McpToolView(val name: String, val description: String, val inputSchema: String)

fun McpTool.toView() = McpToolView(
    name = name.value,
    description = description,
    inputSchema = Json.asFormatString(inputSchema)
)

fun TabContent(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/tools" bind GET to {
        val tools = mcpClient.tools().list()
            .map { list -> list.map { tool -> tool.toView() } }
            .valueOrNull() ?: emptyList()

        elements.mcpTabResponse(
            McpListTabContent(
                items = tools.map { McpListItem(it.name, it.description) },
                typeName = "Tools",
                placeholderText = "Select a tool to view details and call it"
            ),
            McpTabResetSignals()
        )
    }
