/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp.tools

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.mcp.McpFieldView
import org.http4k.wiretap.mcp.mcpDetailResponse
import org.http4k.wiretap.mcp.toFieldViews

data class ToolDetailView(
    val name: String,
    val description: String,
    val fields: List<McpFieldView>
) : ViewModel

fun InspectTool(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/{name}" bind GET to { req ->
        val name = Path.of("name")(req)
        val tool = mcpClient.tools().list()
            .map { list -> list.first { it.name.value == name } }
            .valueOrNull()

        when (tool) {
            null -> Response(NOT_FOUND)
            else -> elements.mcpDetailResponse(
                ToolDetailView(
                    tool.name.value,
                    tool.description,
                    tool.inputSchema.toFieldViews()
                )
            )
        }
    }
