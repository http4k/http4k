package org.http4k.mcp.client

import org.http4k.connect.model.ToolName
import org.http4k.core.Uri
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingHandler
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.Reference
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpTool
import java.time.Duration

/**
 * Client for the MCP protocol. Provides access to the various resources and tools on ths MCP Server
 */
interface McpClient : AutoCloseable {
    fun start(): McpResult<ServerCapabilities>
    fun stop() = close()

    fun tools(): Tools
    fun prompts(): Prompts
    fun progress(): RequestProgress
    fun sampling(): Sampling
    fun resources(): Resources
    fun completions(): Completions

    /**
     * List and interact with Tools provided by this MCP server
     */
    interface Tools {
        fun onChange(fn: () -> Unit)
        fun list(overrideDefaultTimeout: Duration? = null): McpResult<List<McpTool>>
        fun call(
            name: ToolName,
            request: ToolRequest,
            overrideDefaultTimeout: Duration? = null
        ): McpResult<ToolResponse>
    }

    /**
     * List and generate Prompts provided by this MCP server
     */
    interface Prompts {
        fun onChange(fn: () -> Unit)
        fun list(overrideDefaultTimeout: Duration? = null): McpResult<List<McpPrompt>>
        fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration? = null
        ): McpResult<PromptResponse>
    }

    /**
     * Receive progress reports from a Server
     */
    interface RequestProgress {
        fun onProgress(fn: (org.http4k.mcp.model.Progress) -> Unit)
    }

    /**
     * Perform Model Sampling on a model provided by this MCP client. Works during a tool call.
     */
    interface Sampling {
        /**
         * Note that the timeout defined here is applied between each message received by the model
         */
        fun onSampled(overrideDefaultTimeout: Duration? = null, fn: SamplingHandler)
    }

    /**
     * List and interact with Resources provided by this MCP server
     */
    interface Resources {
        fun onChange(fn: () -> Unit)
        fun list(overrideDefaultTimeout: Duration? = null): McpResult<List<McpResource>>
        fun read(request: ResourceRequest, overrideDefaultTimeout: Duration? = null): McpResult<ResourceResponse>
        fun subscribe(uri: Uri, fn: () -> Unit)
        fun unsubscribe(uri: Uri)
    }

    /**
     * Generate Prompt Completions provided by ths MCP Server
     */
    interface Completions {
        fun complete(
            ref: Reference,
            request: CompletionRequest,
            overrideDefaultTimeout: Duration? = null
        ): McpResult<CompletionResponse>
    }
}
