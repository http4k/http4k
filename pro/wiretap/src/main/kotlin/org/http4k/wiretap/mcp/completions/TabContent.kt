/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp.completions

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.client.McpClient
import org.http4k.core.Method.GET
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.mcp.McpTabResetSignals
import org.http4k.wiretap.mcp.mcpTabResponse

data class CompletionPromptView(val name: String, val description: String)

data class CompletionTemplateView(val name: String, val description: String, val uriTemplate: String)

data class TabContent(
    val prompts: List<CompletionPromptView>,
    val templates: List<CompletionTemplateView>
) : ViewModel

fun TabContent(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/" bind GET to {
        val prompts = mcpClient.prompts().list()
            .map { list -> list.map { CompletionPromptView(it.name.value, it.description ?: "") } }
            .valueOrNull() ?: emptyList()

        val templates = mcpClient.resources().listTemplates()
            .map { list -> list.map { CompletionTemplateView(it.name.value, it.description ?: "", it.uriTemplate?.value ?: "") } }
            .valueOrNull() ?: emptyList()

        elements.mcpTabResponse(TabContent(prompts, templates), McpTabResetSignals())
    }
