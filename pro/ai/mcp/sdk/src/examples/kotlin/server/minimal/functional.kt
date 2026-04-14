/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package server.minimal

import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.server.asServer
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.routing.bind
import org.http4k.server.JettyLoom
import java.time.Instant

/**
 * This example demonstrates how to create an minimal MCP tool server using Streaming HTTP protocol.
 */
fun main() {
    val tool = Tool("time", "get the time") bind {
        ToolResponse.Ok(listOf(Content.Text(Instant.now().toString())))
    }

    tool.asServer(JettyLoom(4001)).start()
}

