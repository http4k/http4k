package org.http4k.mcp.client

import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpTool

interface McpClient {
    fun initialize(
        clientInfo: VersionedMcpEntity,
        capabilities: ClientCapabilities = ClientCapabilities(),
        protocolVersion: ProtocolVersion = LATEST_VERSION
    ): Result<ServerCapabilities>

    fun tools(): Tools
    fun prompts(): Prompts
    fun sampling(): Sampling
    fun resources(): Resources
    fun completions(): Completions

    interface Tools {
        fun list(): Result<List<McpTool>>
        fun call(name: ToolName, request: ToolRequest): Result<ToolResponse>
    }

    interface Prompts {
        fun list(): Result<List<McpPrompt>>
        fun get(name: String, request: PromptRequest): Result<PromptResponse>
    }

    interface Sampling {
        fun sample(name: ModelIdentifier, request: SamplingRequest): Sequence<Result<SamplingResponse>>
    }

    interface Resources {
        fun list(): Result<List<McpResource>>
        fun read(name: String, request: ResourceRequest): Result<ResourceResponse>
    }

    interface Completions {
        fun complete(request: CompletionRequest): Result<CompletionResponse>
    }
}
