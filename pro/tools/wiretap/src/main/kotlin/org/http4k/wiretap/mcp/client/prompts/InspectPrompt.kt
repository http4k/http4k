package org.http4k.wiretap.mcp.client.prompts

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.Path
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel

data class PromptDetailView(
    val name: String,
    val description: String,
    val arguments: List<McpPromptArgView>
) : ViewModel

fun InspectPrompt(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/detail/prompts/{name}" bind GET to { req ->
        val name = Path.of("name")(req)
        val prompt = mcpClient.prompts().list()
            .map { list -> list.first { it.name.value == name } }
            .valueOrNull()

        when (prompt) {
            null -> Response(NOT_FOUND)
            else -> Response(OK).datastarElements(
                elements(
                    PromptDetailView(
                        prompt.name.value,
                        prompt.description ?: "",
                        prompt.arguments.map { arg ->
                            McpPromptArgView(arg.name, arg.description ?: "", arg.required ?: false)
                        }
                    )
                ),
                selector = Selector.of("#detail-panel")
            )
        }
    }
