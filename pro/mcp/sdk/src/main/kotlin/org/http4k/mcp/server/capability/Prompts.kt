package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpPrompt.Get
import org.http4k.mcp.util.ObservableList

/**
 * Handles protocol traffic for prompts features.
 */
class Prompts(bindings: List<PromptCapability>) : ObservableList<PromptCapability>(bindings) {

    constructor(vararg bindings: PromptCapability) : this(bindings.toList())

    fun get(req: Get.Request, http: Request) = items
        .find { it.toPrompt().name == req.name }
        ?.get(req, http)
        ?: throw McpException(InvalidParams)

    fun list(mcp: McpPrompt.List.Request, http: Request) =
        McpPrompt.List.Response(items.map(PromptCapability::toPrompt))
}

