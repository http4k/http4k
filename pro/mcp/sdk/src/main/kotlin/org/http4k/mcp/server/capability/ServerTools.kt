package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.server.protocol.Client
import org.http4k.mcp.server.protocol.Tools
import org.http4k.mcp.util.ObservableList

class ServerTools(list: Iterable<ToolCapability>) : ObservableList<ToolCapability>(list), Tools {
    constructor(vararg list: ToolCapability) : this(list.toList())

    override fun list(req: McpTool.List.Request, http: Request): McpTool.List.Response =
        McpTool.List.Response(items.map(ToolCapability::toTool))

    override fun call(req: McpTool.Call.Request, client: Client, http: Request): McpTool.Call.Response = items
        .find { it.toTool().name == req.name }
        ?.call(req, client, http)
        ?: throw McpException(InvalidParams)
}
