/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.LogLevel.info
import org.http4k.ai.mcp.model.Tool
import org.http4k.routing.bind

fun loggingTool() =
    Tool("test_tool_with_logging", "test_tool_with_logging") bind {
        it.client.log("Tool execution started", info)
        it.client.log("Tool processing data", info)
        it.client.log("Tool execution completed", info)

        Ok(textContent)
    }

