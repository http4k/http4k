package org.http4k.wiretap.mcp.client.tools

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
import org.http4k.wiretap.util.Json

data class ToolDetailView(val name: String, val description: String, val inputSchema: String) : ViewModel

fun InspectTool(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/detail/tools/{name}" bind GET to { req ->
        val name = Path.of("name")(req)
        val tool = mcpClient.tools().list()
            .map { list -> list.first { it.name.value == name } }
            .valueOrNull()

        when (tool) {
            null -> Response(NOT_FOUND)
            else -> Response(OK).datastarElements(
                elements(ToolDetailView(tool.name.value, tool.description, Json.asFormatString(tool.inputSchema))),
                selector = Selector.of("#detail-panel")
            )
        }
    }
