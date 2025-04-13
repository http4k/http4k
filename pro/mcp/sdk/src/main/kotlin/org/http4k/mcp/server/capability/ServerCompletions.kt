package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.Client
import org.http4k.mcp.server.protocol.Completions

class ServerCompletions(private val bindings: Iterable<CompletionCapability>) : Completions {

    constructor(vararg bindings: CompletionCapability) : this(bindings.toList())

    override fun complete(mcp: McpCompletion.Request, client: Client, http: Request) =
        bindings.find { it.toReference() == mcp.ref }
            ?.complete(mcp, client, http)
            ?: throw McpException(InvalidParams)
}
