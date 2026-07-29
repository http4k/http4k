/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.tools

import org.http4k.ai.mcp.stateless.ToolResponse
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.lens.stateless.MetaKey
import org.http4k.lens.stateless.progressToken
import org.http4k.routing.stateless.bind

fun progressTool() = Tool("test_tool_with_progress", "test_tool_with_progress") bind {
    val progressToken = MetaKey.progressToken<Any>().toLens()(it.meta) ?: "unknown"
    it.client.progress(progressToken, 0, 100.0, "Completed step 0 of 100")
    it.client.progress(progressToken, 50, 100.0, "Completed step 50 of 100")
    it.client.progress(progressToken, 100, 100.0, "Completed step 100 of 100")

    ToolResponse.Ok(textContent)
}
