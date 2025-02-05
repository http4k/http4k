package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.util.ObservableList

/**
 * Handles protocol traffic for server provided tools.
 */
class Tools(list: List<ToolCapability>) : ObservableList<ToolCapability>(list) {

    fun list(req: McpTool.List.Request, http: Request) = McpTool.List.Response(items.map(ToolCapability::toTool))

    fun call(req: McpTool.Call.Request, http: Request) = items
        .find { it.toTool().name == req.name }
        ?.call(req, http)
        ?: throw McpException(InvalidParams)
}
