/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.tools

import org.http4k.ai.mcp.stateless.ToolResponse.Ok
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.lens.stateless.MetaKey
import org.http4k.lens.stateless.progressToken
import org.http4k.routing.stateless.bind

fun testStreamingElicitationTool() =
    Tool("test_streaming_elicitation", "test_streaming_elicitation") bind {
        val progressToken = MetaKey.progressToken<Any>().toLens()(it.meta) ?: "unknown"
        it.client.progress(progressToken, 50, 100.0, "streaming")

        Ok(textContent)
    }
