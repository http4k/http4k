package org.http4k.mcp.server

import org.http4k.core.Request
import org.http4k.mcp.model.Completion
import org.http4k.mcp.protocol.McpCompletion

class McpCompletions {
    fun complete(req: McpCompletion.Request, http: Request) = McpCompletion.Response(Completion(listOf()))
}
