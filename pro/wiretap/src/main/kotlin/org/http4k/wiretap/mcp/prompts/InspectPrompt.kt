package org.http4k.wiretap.mcp.prompts

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.PromptName
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.mcp.mcpDetailResponse

data class PromptDetailView(
    val name: String,
    val description: String,
    val arguments: List<McpPromptArgView>
) : ViewModel

fun InspectPrompt(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/{name}" bind GET to { req ->
        val name = Path.value(PromptName).of("name")(req)
        val prompt = mcpClient.prompts().list()
            .map { list -> list.first { it.name == name } }
            .valueOrNull()

        when (prompt) {
            null -> Response(NOT_FOUND)
            else -> elements.mcpDetailResponse(
                PromptDetailView(
                    prompt.name.value,
                    prompt.description ?: "",
                    prompt.arguments.map { arg ->
                        McpPromptArgView(arg.name, arg.description ?: "", arg.required ?: false)
                    }
                )
            )
        }
    }
