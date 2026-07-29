/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.client

import org.http4k.ai.mcp.stateless.CompletionRequest
import org.http4k.ai.mcp.stateless.CompletionResponse
import org.http4k.ai.mcp.stateless.McpResult
import org.http4k.ai.mcp.stateless.PromptRequest
import org.http4k.ai.mcp.stateless.PromptResponse
import org.http4k.ai.mcp.stateless.ResourceRequest
import org.http4k.ai.mcp.stateless.ResourceResponse
import org.http4k.ai.mcp.stateless.ToolRequest
import org.http4k.ai.mcp.stateless.ToolResponse
import org.http4k.ai.mcp.stateless.model.LogMessage
import org.http4k.ai.mcp.stateless.model.Progress
import org.http4k.ai.mcp.stateless.model.PromptName
import org.http4k.ai.mcp.stateless.model.Reference
import org.http4k.ai.mcp.stateless.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.stateless.protocol.messages.McpPrompt
import org.http4k.ai.mcp.stateless.protocol.messages.McpResource
import org.http4k.ai.mcp.stateless.protocol.messages.McpTool
import org.http4k.ai.model.ToolName
import org.http4k.core.Uri

interface McpClient : AutoCloseable {

    fun stop() = close()

    fun discover(): McpResult<VersionedMcpEntity>

    fun tools(): Tools
    fun prompts(): Prompts
    fun resources(): Resources
    fun completions(): Completions

    interface Tools {
        fun list(): McpResult<List<McpTool>>

        // onProgress/onLog default to null (no-op): providing either streams the response (Accept:
        // text/event-stream), diverting notifications/progress + notifications/message to the callbacks.
        fun call(
            name: ToolName,
            request: ToolRequest = ToolRequest(), onProgress: ((Progress) -> Unit)? = null,
            onLog: ((LogMessage) -> Unit)? = null
        ): McpResult<ToolResponse>

        fun onListChanged(handler: () -> Unit): McpResult<AutoCloseable>
    }

    interface Prompts {
        fun list(): McpResult<List<McpPrompt>>
        fun get(
            name: PromptName,
            request: PromptRequest, onProgress: ((Progress) -> Unit)? = null,
            onLog: ((LogMessage) -> Unit)? = null
        ): McpResult<PromptResponse>

        fun onListChanged(handler: () -> Unit): McpResult<AutoCloseable>
    }

    interface Resources {
        fun list(): McpResult<List<McpResource>>
        fun listTemplates(): McpResult<List<McpResource>>
        fun read(
            request: ResourceRequest, onProgress: ((Progress) -> Unit)? = null,
            onLog: ((LogMessage) -> Unit)? = null
        ): McpResult<ResourceResponse>

        fun onListChanged(handler: () -> Unit): McpResult<AutoCloseable>

        fun subscribe(uri: Uri, handler: () -> Unit): McpResult<AutoCloseable>
    }

    interface Completions {
        fun complete(
            ref: Reference,
            request: CompletionRequest
        ): McpResult<CompletionResponse>
    }
}
