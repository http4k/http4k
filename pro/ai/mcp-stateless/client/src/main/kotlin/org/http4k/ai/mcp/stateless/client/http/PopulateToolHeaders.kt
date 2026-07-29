/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.client.http

import org.http4k.ai.mcp.stateless.protocol.encodeMcpHeaderValue
import org.http4k.ai.mcp.stateless.protocol.messages.McpTool
import org.http4k.ai.model.ToolName
import org.http4k.core.Filter

internal fun PopulateToolHeaders(headerParams: Map<String, String>, arguments: Map<String, Any>) = Filter { next ->
    {
        next(headerParams.entries.fold(it) { req, (param, header) ->
            arguments[param]?.let { value -> req.header("Mcp-Param-$header", encodeMcpHeaderValue(value.toString())) } ?: req
        })
    }
}

@Suppress("UNCHECKED_CAST")
internal fun List<McpTool>.headerParamsByName(): Map<ToolName, Map<String, String>> = associate { tool ->
    tool.name to (tool.inputSchema["properties"] as? Map<String, Any>).orEmpty()
        .mapNotNull { (param, schema) -> ((schema as? Map<String, Any>)?.get("x-mcp-header") as? String)?.let { param to it } }
        .toMap()
}
