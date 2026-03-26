/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp.completions

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.core.Method.GET
import org.http4k.lens.Path
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.mcp.mcpDetailResponse

data class CompletionDetailView(
    val name: String,
    val refType: String,
    val refName: String,
    val argumentNames: List<String>
) : ViewModel {
    val firstArgumentName get() = argumentNames.firstOrNull() ?: ""
}

fun InspectPromptCompletion(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/prompts/{name}" bind GET to { req ->
        val name = Path.string().of("name")(req)

        val argumentNames = mcpClient.prompts().list()
            .map { prompts -> prompts.firstOrNull { it.name.value == name }?.arguments?.map { it.name } ?: emptyList() }
            .valueOrNull() ?: emptyList()

        elements.mcpDetailResponse(
            CompletionDetailView(
                name = name,
                refType = "prompt",
                refName = name,
                argumentNames = argumentNames
            )
        )
    }

fun InspectTemplateCompletion(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/templates/{name}" bind GET to { req ->
        val name = Path.string().of("name")(req)

        val argumentNames = mcpClient.resources().listTemplates()
            .map { templates ->
                templates.firstOrNull { it.name.value == name }
                    ?.uriTemplate?.value
                    ?.let { extractTemplateParams(it) }
                    ?: emptyList()
            }
            .valueOrNull() ?: emptyList()

        elements.mcpDetailResponse(
            CompletionDetailView(
                name = name,
                refType = "template",
                refName = name,
                argumentNames = argumentNames
            )
        )
    }

internal fun extractTemplateParams(template: String): List<String> =
    Regex("\\{\\+?(\\w+)}").findAll(template).map { it.groupValues[1] }.toList()
