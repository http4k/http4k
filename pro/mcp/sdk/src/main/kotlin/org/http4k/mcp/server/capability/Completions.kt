package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpCompletion

class Completions(private val bindings: List<CompletionCapability>) {
    fun complete(mcp: McpCompletion.Request, http: Request) =
        bindings.find { it.toReference() == mcp.ref }
            ?.complete(mcp, http)
            ?: throw McpException(InvalidParams)
}
