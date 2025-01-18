package org.http4k.mcp

import org.http4k.connect.mcp.Completion
import org.http4k.connect.mcp.McpCompletion

class McpCompletions {
    fun complete(req: McpCompletion.Request) = McpCompletion.Response(Completion(listOf()))
}
