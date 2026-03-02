package org.http4k.wiretap.mcp.client.prompts

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.html
import org.http4k.routing.bind
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.Json

data class McpClientPromptsSignals(val selectedItem: String = "")

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

fun Index(mcpClient: McpClient, html: TemplateRenderer) = "/prompts" bind GET to {
    val prompts = mcpClient.prompts().list()
        .map { list -> list.map { prompt -> prompt.toView() } }
        .valueOrNull() ?: emptyList()

    Response(OK).html(html(Index(prompts)))
}

data class Index(val prompts: List<McpPromptView>) : ViewModel {
    val initialSignals: String = Json.asFormatString(McpClientPromptsSignals())
}

private fun McpPrompt.toView() = McpPromptView(
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
