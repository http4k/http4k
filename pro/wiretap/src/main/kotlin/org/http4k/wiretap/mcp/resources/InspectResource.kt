/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp.resources

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
import org.http4k.wiretap.mcp.mcpDetailResponse

data class ResourceDetailView(
    val uri: String,
    val name: String,
    val description: String,
    val mimeType: String
) : ViewModel

fun InspectResource(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/{name}" bind GET to { req ->
        val name = Path.of("name")(req)
        val resource = mcpClient.resources().list()
            .map { list -> list.first { it.name.value == name } }
            .valueOrNull()

        when (resource) {
            null -> Response(NOT_FOUND)
            else -> elements.mcpDetailResponse(
                ResourceDetailView(
                    resource.uri?.toString() ?: "",
                    resource.name.value,
                    resource.description ?: "",
                    resource.mimeType?.value ?: ""
                )
            )
        }
    }
