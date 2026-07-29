/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.capability

import org.http4k.ai.mcp.stateless.Client
import org.http4k.ai.mcp.stateless.PromptFilter
import org.http4k.ai.mcp.stateless.PromptHandler
import org.http4k.ai.mcp.stateless.PromptRequest
import org.http4k.ai.mcp.stateless.PromptResponse.Error
import org.http4k.ai.mcp.stateless.PromptResponse.InputRequired
import org.http4k.ai.mcp.stateless.PromptResponse.Ok
import org.http4k.ai.mcp.stateless.model.Prompt
import org.http4k.ai.mcp.stateless.model.ResultType
import org.http4k.ai.mcp.stateless.protocol.McpException
import org.http4k.ai.mcp.stateless.protocol.messages.DomainError
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt
import org.http4k.ai.mcp.stateless.then
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

    fun get(mcp: McpPrompt.Get.Request.Params, client: Client, http: Request) = try {
        when (val result = handler(
            PromptRequest(
                mcp.arguments, mcp._meta, client, http, mcp.inputResponses.toElicitationResponses(), mcp.requestState
            )
        )) {
            is Ok -> McpPrompt.Get.Response.Result(
                result.messages, result.description, ttlMs = result.ttlMs, cacheScope = prompt.cacheScope
            )

            is Error -> throw McpException(DomainError(result.message))

            is InputRequired -> McpPrompt.Get.Response.Result(
                emptyList(),
                resultType = ResultType.input_required,
                inputRequests = result.inputRequests.toWireRequests(mcp._meta.clientCapabilities()),
                requestState = result.requestState
            )
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
