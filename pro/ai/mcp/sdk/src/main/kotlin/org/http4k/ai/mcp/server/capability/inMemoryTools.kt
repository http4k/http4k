/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.protocol.Tools
import org.http4k.ai.mcp.util.ObservableList
import org.http4k.ai.model.ToolName
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound

fun tools(vararg tools: ToolCapability): Tools = tools(tools.toList())

fun tools(list: Iterable<ToolCapability>): Tools = InMemoryTools(list)

private class InMemoryTools(list: Iterable<ToolCapability>) : ObservableList<ToolCapability>(list), Tools {
    override fun list(req: McpTool.List.Request.Params, client: Client, http: Request): McpTool.List.Response.Result =
        McpTool.List.Response.Result(items.map(ToolCapability::toTool))

    override fun call(req: McpTool.Call.Request.Params, client: Client, http: Request): McpTool.Call.Response.Result = items
        .find { it.toTool().name == req.name }
        ?.call(req, client, http)
        ?: throw McpException(InvalidParams)

    override fun invoke(name: ToolName) = items.find { it.toTool().name == name } ?: throw McpException(MethodNotFound)
}

