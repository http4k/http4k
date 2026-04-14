/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package server.minimal

import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.asServer
import org.http4k.ai.mcp.server.capability.CompletionCapability
import org.http4k.ai.mcp.server.capability.completions
import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.server.JettyLoom
import java.time.Instant

/**
 * This example demonstrates how to create an minimal MCP tool server using the SSE-only protocol.
 */
fun main() {
    val tools = tools(
        Tool("time", "get the time") bind {
            ToolResponse.Ok(listOf(Content.Text(Instant.now().toString())))
        }
    )

    val completions = completions(
        Reference.Prompt("prompt1") bind {
            CompletionResponse.Ok(listOf())
        }
    )

    val prompts = prompts(Prompt("prompt1", "description1") bind {
        PromptResponse.Ok(listOf(), "description")
    })

    val resources = resources(Resource.Static("https://www.http4k.org", "HTTP4K", "description") bind {
        ResourceResponse.Ok(Resource.Content.Text("hello", Uri.of("https://www.http4k.org")))
    })

    (completions + tools + prompts + resources).asServer(JettyLoom(4001)).start()
}

