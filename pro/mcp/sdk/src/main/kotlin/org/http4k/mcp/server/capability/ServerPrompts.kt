package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpPrompt.Get
import org.http4k.mcp.Client
import org.http4k.mcp.server.protocol.Prompts
import org.http4k.mcp.util.ObservableList


class ServerPrompts(bindings: Iterable<PromptCapability>) : ObservableList<PromptCapability>(bindings), Prompts {

    constructor(vararg bindings: PromptCapability) : this(bindings.toList())

    override fun get(req: Get.Request, client: Client, http: Request) = items
        .find { it.toPrompt().name == req.name }
        ?.get(req, client, http)
        ?: throw McpException(InvalidParams)

    override fun list(mcp: McpPrompt.List.Request, client: Client, http: Request) =
        McpPrompt.List.Response(items.map(PromptCapability::toPrompt))
}

