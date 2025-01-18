package org.http4k.mcp.server

import org.http4k.connect.mcp.protocol.McpCompletion
import org.http4k.mcp.model.Completion

class McpCompletions {
    fun complete(req: McpCompletion.Request) = McpCompletion.Response(Completion(listOf()))
}
