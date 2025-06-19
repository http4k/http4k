package org.http4k.ai.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.messages.McpCompletion

interface Completions {
    fun complete(mcp: McpCompletion.Request, client: Client, http: Request): McpCompletion.Response
}
