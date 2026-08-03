/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Tool
import org.http4k.lens.MetaKey
import org.http4k.lens.progressToken
import org.http4k.routing.bind

// SEP-2575: the request stream must carry only IncompleteResult chunks (notifications + the final result),
// never an independent JSON-RPC request. Emitting a progress notification then the result exercises that.
fun testStreamingElicitationTool() =
    Tool("test_streaming_elicitation", "test_streaming_elicitation") bind {
        val progressToken = MetaKey.progressToken<Any>().toLens()(it.meta) ?: "unknown"
        it.client.progress(progressToken, 50, 100.0, "streaming")

        Ok(textContent)
    }
