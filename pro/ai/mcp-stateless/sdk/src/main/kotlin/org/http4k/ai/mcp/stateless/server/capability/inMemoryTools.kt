/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.capability

import org.http4k.ai.mcp.stateless.Client
import org.http4k.ai.mcp.stateless.model.CacheScope
import org.http4k.ai.mcp.stateless.model.CacheScope.public
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.ai.mcp.stateless.protocol.McpException
import org.http4k.ai.mcp.stateless.protocol.messages.McpTool
import org.http4k.ai.mcp.stateless.server.protocol.Tools
import org.http4k.ai.mcp.stateless.util.ObservableList
import org.http4k.ai.model.ToolName
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound

fun tools(vararg tools: ToolCapability, ttlMs: TtlMs = TtlMs.of(0), cacheScope: CacheScope = public): Tools =
    tools(tools.toList(), ttlMs, cacheScope)

fun tools(list: Iterable<ToolCapability>, ttlMs: TtlMs = TtlMs.of(0), cacheScope: CacheScope = public): Tools =
    InMemoryTools(list, ttlMs, cacheScope)

private class InMemoryTools(
    list: Iterable<ToolCapability>,
    private val ttlMs: TtlMs,
    private val cacheScope: CacheScope,
) : ObservableList<ToolCapability>(list), Tools {
    override fun list(req: McpTool.List.Request.Params, client: Client, http: Request): McpTool.List.Response.Result =
        McpTool.List.Response.Result(
            items.map(ToolCapability::toTool).sortedBy { it.name.value }, ttlMs = ttlMs, cacheScope = cacheScope
        )

    override fun call(req: McpTool.Call.Request.Params, client: Client, http: Request): McpTool.Call.Response.Result = items
        .find { it.tool.name == req.name }
        ?.call(req, client, http)
        ?: throw McpException(InvalidParams)

    override fun invoke(name: ToolName) = items.find { it.tool.name == name } ?: throw McpException(MethodNotFound)
}
