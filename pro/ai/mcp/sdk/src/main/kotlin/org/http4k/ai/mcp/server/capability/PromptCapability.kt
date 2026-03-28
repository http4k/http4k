/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.PromptFilter
import org.http4k.ai.mcp.PromptHandler
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse.Error
import org.http4k.ai.mcp.PromptResponse.Ok
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.DomainError
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.then
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.LensFailure

class PromptCapability(
    internal val prompt: Prompt,
    internal val handler: PromptHandler
) : ServerCapability, PromptHandler {
    override val name = prompt.name.value

    fun toPrompt() = McpPrompt(prompt.name, prompt.description, prompt.title, prompt.args.map {
        McpPrompt.Argument(it.meta.name, it.meta.description, it.meta.metadata["title"] as String?, it.meta.required)
    }, prompt.icons)

    fun get(mcp: McpPrompt.Get.Request, client: Client, http: Request) = try {
        when (val result = handler(PromptRequest(mcp.arguments, mcp._meta, client, http))) {
            is Ok -> McpPrompt.Get.Response(result.messages, result.description)
            is Error -> throw McpException(DomainError(result.message))
        }
    } catch (e: McpException) {
        throw e
    } catch (e: LensFailure) {
        throw McpException(InvalidParams, e)
    } catch (e: Exception) {
        throw McpException(InternalError, e)
    }

    override fun invoke(p1: PromptRequest) = handler(p1)
}

fun PromptFilter.then(capability: PromptCapability) = PromptCapability(capability.prompt, then(capability))
