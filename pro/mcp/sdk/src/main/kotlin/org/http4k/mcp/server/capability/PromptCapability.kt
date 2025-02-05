package org.http4k.mcp.server.capability

import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.LensFailure
import org.http4k.mcp.PromptHandler
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.model.Prompt
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpPrompt

class PromptCapability(private val prompt: Prompt, val handler: PromptHandler) : ServerCapability {
    fun toPrompt() = McpPrompt(prompt.name, prompt.description, prompt.args.map {
        McpPrompt.Argument(it.meta.name, it.meta.description, it.meta.required)
    })

    fun get(mcp: McpPrompt.Get.Request, http: Request) = try {
        handler(PromptRequest(mcp.arguments, http))
            .let { McpPrompt.Get.Response(it.messages, it.description) }
    } catch (e: LensFailure) {
        throw McpException(InvalidParams, e)
    } catch (e: Exception) {
        throw McpException(InternalError, e)
    }
}
