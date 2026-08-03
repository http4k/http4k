/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.LogLevel.info
import org.http4k.ai.mcp.model.Tool
import org.http4k.routing.bind

// SEP-2575: a tool that attempts to log must emit no notifications/message when the request did not set
// _meta.../logLevel. StreamingClient.log gates on logLevel, so this attempt is silently dropped.
fun testLoggingTool() =
    Tool("test_logging_tool", "test_logging_tool") bind {
        it.client.log("Tool attempted to log", info)

        Ok(textContent)
    }
