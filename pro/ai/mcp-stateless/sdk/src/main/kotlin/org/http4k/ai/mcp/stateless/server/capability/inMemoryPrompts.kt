/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.capability

import org.http4k.ai.mcp.stateless.Client
import org.http4k.ai.mcp.stateless.model.CacheScope
import org.http4k.ai.mcp.stateless.model.CacheScope.public
import org.http4k.ai.mcp.stateless.model.PromptName
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.ai.mcp.stateless.protocol.McpException
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt.Get
import org.http4k.ai.mcp.stateless.server.protocol.Prompts
import org.http4k.ai.mcp.stateless.util.ObservableList
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound

fun prompts(vararg capabilities: PromptCapability, ttlMs: TtlMs = TtlMs.of(0), cacheScope: CacheScope = public): Prompts =
    prompts(capabilities.toList(), ttlMs, cacheScope)

fun prompts(capabilities: Iterable<PromptCapability>, ttlMs: TtlMs = TtlMs.of(0), cacheScope: CacheScope = public): Prompts =
    InMemoryPrompts(capabilities, ttlMs, cacheScope)

private class InMemoryPrompts(
    capabilities: Iterable<PromptCapability>,
    private val ttlMs: TtlMs,
    private val cacheScope: CacheScope,
) : ObservableList<PromptCapability>(capabilities), Prompts {
    private val byName = derived { it.associateBy { prompt -> prompt.prompt.name } }
    private val listed = derived { it.map(PromptCapability::toPrompt) }

    override fun get(req: Get.Request.Params, client: Client, http: Request) =
        byName()[req.name]?.get(req, client, http) ?: throw McpException(InvalidParams)

    override fun list(mcp: McpPrompt.List.Request.Params, client: Client, http: Request) =
        McpPrompt.List.Response.Result(listed(), ttlMs = ttlMs, cacheScope = cacheScope)

    override fun invoke(name: PromptName) = byName()[name] ?: throw McpException(MethodNotFound)
}
