/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client

import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.LogMessage
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import java.time.Duration

/**
 * Stateless (2026-07-28) MCP client: each call is an independent POST. No handshake, no session,
 * no server->client notifications (those return with subscriptions/listen + MRTR in later stages).
 */
interface McpClient : AutoCloseable {

    fun start(overrideDefaultTimeout: Duration? = null): McpResult<Unit>
    fun stop() = close()

    fun discover(overrideDefaultTimeout: Duration? = null): McpResult<VersionedMcpEntity>

    fun tools(): Tools
    fun prompts(): Prompts
    fun resources(): Resources
    fun completions(): Completions

    interface Tools {
        fun list(overrideDefaultTimeout: Duration? = null): McpResult<List<McpTool>>

        // onProgress/onLog default to null (no-op): providing either streams the response (Accept:
        // text/event-stream), diverting notifications/progress + notifications/message to the callbacks.
        fun call(
            name: ToolName,
            request: ToolRequest = ToolRequest(),
            overrideDefaultTimeout: Duration? = null,
            onProgress: ((Progress) -> Unit)? = null,
            onLog: ((LogMessage) -> Unit)? = null
        ): McpResult<ToolResponse>

        fun onListChanged(handler: () -> Unit): McpResult<AutoCloseable>
    }

    interface Prompts {
        fun list(overrideDefaultTimeout: Duration? = null): McpResult<List<McpPrompt>>
        fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration? = null,
            onProgress: ((Progress) -> Unit)? = null,
            onLog: ((LogMessage) -> Unit)? = null
        ): McpResult<PromptResponse>

        fun onListChanged(handler: () -> Unit): McpResult<AutoCloseable>
    }

    interface Resources {
        fun list(overrideDefaultTimeout: Duration? = null): McpResult<List<McpResource>>
        fun listTemplates(overrideDefaultTimeout: Duration? = null): McpResult<List<McpResource>>
        fun read(
            request: ResourceRequest,
            overrideDefaultTimeout: Duration? = null,
            onProgress: ((Progress) -> Unit)? = null,
            onLog: ((LogMessage) -> Unit)? = null
        ): McpResult<ResourceResponse>

        fun onListChanged(handler: () -> Unit): McpResult<AutoCloseable>

        fun subscribe(uri: Uri, handler: () -> Unit): McpResult<AutoCloseable>
    }

    interface Completions {
        fun complete(
            ref: Reference,
            request: CompletionRequest,
            overrideDefaultTimeout: Duration? = null
        ): McpResult<CompletionResponse>
    }
}
