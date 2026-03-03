package org.http4k.wiretap.mcp.prompts

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel

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

data class TabContent(val prompts: List<McpPromptView>) : ViewModel

fun PromptsTabContent(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/tab/prompts" bind GET to {
        val prompts = mcpClient.prompts().list()
            .map { list -> list.map { prompt -> prompt.toView() } }
            .valueOrNull() ?: emptyList()

        Response(OK).datastarElements(
            elements(TabContent(prompts)),
            selector = Selector.of("#mcp-content")
        )
    }
