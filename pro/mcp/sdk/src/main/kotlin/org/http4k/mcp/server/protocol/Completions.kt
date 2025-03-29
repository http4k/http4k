package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.protocol.messages.McpCompletion

interface Completions {
    fun complete(mcp: McpCompletion.Request, client: Client, http: Request): McpCompletion.Response
}
