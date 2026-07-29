/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.model.ToolName
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.http4k.lens.MCP_NAME

class PopulateToolHeaders(
    private val lastTools: List<McpTool>,
    private val method: McpRpcMethod,
    private val name: ToolName,
    private val arguments: Map<String, Any>
) : Filter {
    override fun invoke(next: HttpHandler): HttpHandler = { request ->
        val base = request
            .with(Header.MCP_METHOD of method)
            .with(Header.MCP_NAME of name.value)

        next(
            lastTools.find { it.name == name }
                ?.let { tool ->
                    tool.mcpHeaderAnnotations().fold(base) { req, param ->
                        arguments[param.first]?.let {
                            req.header("Mcp-Param-${param.second}", it.toString())
                        } ?: req
                    }
                } ?: base
        )
    }
}

@Suppress("UNCHECKED_CAST")
private fun McpTool.mcpHeaderAnnotations(): List<Pair<String, String>> =
    (inputSchema["properties"] as? Map<String, Any>)?.let { properties ->
        properties.mapNotNull { (paramName, schema) ->
            (schema as? Map<String, Any>)?.let {
                (it["x-mcp-header"] as? String)?.let { headerName ->
                    paramName to headerName
                }
            }
        }
    } ?: emptyList()
