/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.model.Tool
import org.http4k.routing.bind

fun dynamicTool() = Tool("test_dynamic_tool", "test_dynamic_tool") bind {
    Error("Not implemented yet")
}
