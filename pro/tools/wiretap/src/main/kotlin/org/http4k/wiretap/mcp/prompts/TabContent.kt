package org.http4k.wiretap.mcp.prompts

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.core.Method.GET
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.wiretap.mcp.McpListItem
import org.http4k.wiretap.mcp.McpListTabContent
import org.http4k.wiretap.mcp.McpTabResetSignals
import org.http4k.wiretap.mcp.mcpTabResponse

data class McpPromptView(
    val name: String,
    val description: String,
    val arguments: List<McpPromptArgView>
)

data class McpPromptArgView(
    val name: String,
    val description: String,
    val required: Boolean
)

fun McpPrompt.toView() = McpPromptView(
    name = name.value,
    description = description ?: "",
    arguments = arguments.map { arg ->
        McpPromptArgView(
            name = arg.name,
            description = arg.description ?: "",
            required = arg.required ?: false
        )
    }
)

fun TabContent(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/prompts" bind GET to {
        val prompts = mcpClient.prompts().list()
            .map { list -> list.map { prompt -> prompt.toView() } }
            .valueOrNull() ?: emptyList()

        elements.mcpTabResponse(
            McpListTabContent(
                items = prompts.map { McpListItem(it.name, it.description) },
                typeName = "Prompts",
                placeholderText = "Select a prompt to view details and get it"
            ),
            McpTabResetSignals()
        )
    }
