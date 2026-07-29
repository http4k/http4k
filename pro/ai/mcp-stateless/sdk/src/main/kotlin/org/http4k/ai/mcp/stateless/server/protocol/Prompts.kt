/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.protocol

import org.http4k.ai.mcp.stateless.Client
import org.http4k.ai.mcp.stateless.PromptHandler
import org.http4k.ai.mcp.stateless.model.PromptName
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt
import org.http4k.ai.mcp.stateless.server.capability.PromptCapability
import org.http4k.core.Request

/**
 * Handles protocol traffic for prompts features.
 */
interface Prompts : ObservableCapability<PromptCapability>, (PromptName) -> PromptHandler, Iterable<PromptCapability> {
    fun get(req: McpPrompt.Get.Request.Params, client: Client, http: Request): McpPrompt.Get.Response.Result
    fun list(mcp: McpPrompt.List.Request.Params, client: Client, http: Request): McpPrompt.List.Response.Result
}
