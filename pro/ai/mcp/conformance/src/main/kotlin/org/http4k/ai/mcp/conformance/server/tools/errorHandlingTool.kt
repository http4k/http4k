/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.model.Tool
import org.http4k.routing.bind

fun errorHandlingTool() = Tool("test_error_handling", "test_error_handling") bind {
//    ToolResponse.Error(1, "This tool intentionally returns an error for testing")
    throw Exception("This tool intentionally returns an error for testing")
}

