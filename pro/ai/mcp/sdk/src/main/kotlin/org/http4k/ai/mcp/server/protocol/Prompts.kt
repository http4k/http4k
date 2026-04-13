/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.PromptHandler
import org.http4k.ai.mcp.ToolHandler
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.model.ToolName
import org.http4k.core.Request

/**
 * Handles protocol traffic for prompts features.
 */
interface Prompts : ObservableCapability<PromptCapability>, (Prompt) -> PromptHandler {
    fun get(req: McpPrompt.Get.Request, client: Client, http: Request): McpPrompt.Get.Response
    fun list(mcp: McpPrompt.List.Request, client: Client, http: Request): McpPrompt.List.Response
}
