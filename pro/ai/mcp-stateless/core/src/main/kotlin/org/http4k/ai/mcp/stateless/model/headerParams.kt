/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.model

import org.http4k.ai.mcp.stateless.ToolRequest

const val X_MCP_HEADER = "x-mcp-header"

fun fromHeader(name: String): Map<String, Any> = mapOf(X_MCP_HEADER to name)

fun Tool.mirroredHeaderArgs() = args.mapNotNull { arg ->
    arg.meta.metadata[X_MCP_HEADER]?.let { arg.meta.name to it.toString() }
}

internal fun McpCapabilityLens<ToolRequest, *>.headerName() = meta.metadata[X_MCP_HEADER]?.toString()
