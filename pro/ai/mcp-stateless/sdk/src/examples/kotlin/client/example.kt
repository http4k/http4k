/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package client

import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.model.ToolName
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Uri

fun main() {
    val mcpClient = HttpStreamingMcpClient(
        Uri.of("http://localhost:3001/mcp"),
        McpEntity.of("foobar"), Version.of("1.0.0"),
        JavaHttpClient(responseBodyMode = Stream),
        ClientCapabilities()
    )

    println(mcpClient.start())

    println(mcpClient.prompts().list())
    println(mcpClient.prompts().get(PromptName.of("prompt2"), PromptRequest(mapOf("a1" to "foo"))))

    println(mcpClient.resources().list())
    println(mcpClient.resources().read(ResourceRequest(Uri.of("https://www.http4k.org"))))

    println(
        mcpClient.completions()
            .complete(Reference.Prompt("prompt2"), CompletionRequest(CompletionArgument("foo", "bar")))
    )

    println(mcpClient.tools().list().valueOrNull())
    println(mcpClient.tools().call(ToolName.of("weather"), ToolRequest(mapOf("city" to "london"))))

    mcpClient.stop()
}
