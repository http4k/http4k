package org.http4k.ai.mcp.client.http

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.McpError.Http
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.PromptRequest
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.asAOrFailure
import org.http4k.ai.mcp.client.toHttpRequest
import org.http4k.ai.mcp.model.Progress
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerCapabilities
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.McpCompletion
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.http4k.ai.mcp.protocol.messages.McpResource
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.protocol.messages.ServerMessage
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.asFormatString
import org.http4k.ai.mcp.util.McpJson.convert
import org.http4k.ai.model.ToolName
import org.http4k.client.JavaHttpClient
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.lens.accept
import org.http4k.sse.SseMessage.Event
import java.time.Duration

/**
 * JSON Rpc connection MCP client.
 */
class HttpNonStreamingMcpClient(
    private val baseUri: Uri,
    private val http: HttpHandler = JavaHttpClient(),
    private val protocolVersion: ProtocolVersion = LATEST_VERSION,
) :
    McpClient {

    override fun start() = Success(ServerCapabilities())

    override fun progress() = object : McpClient.RequestProgress {
        override fun onProgress(fn: (Progress) -> Unit) =
            throw UnsupportedOperationException()
    }

    override fun tools() = object : McpClient.Tools {
        override fun onChange(fn: () -> Unit) = throw UnsupportedOperationException()

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpTool.List.Response>(McpTool.List, McpTool.List.Request())
                .map { it.tools }

        override fun call(
            name: ToolName,
            request: ToolRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send<McpTool.Call.Response>(
            McpTool.Call,
            McpTool.Call.Request(name, request.mapValues { McpJson.asJsonObject(it.value) })
        )
            .map {
                when (it.isError) {
                    true -> Error(
                        ErrorMessage(
                            -1, it.content?.joinToString()
                                ?: it.structuredContent?.let(::asFormatString)
                                ?: "<no message"
                        )
                    )

                    else -> Ok(
                        it.content,
                        it.structuredContent?.let(::convert),
                        it._meta
                    )
                }
            }
    }

    override fun prompts() = object : McpClient.Prompts {
        override fun onChange(fn: () -> Unit) = throw UnsupportedOperationException()

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpPrompt.List.Response>(McpPrompt.List, McpPrompt.List.Request())
                .map { it.prompts }

        override fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send<McpPrompt.Get.Response>(McpPrompt.Get, McpPrompt.Get.Request(name, request))
            .map { PromptResponse(it.messages, it.description) }
    }

    override fun sampling() = throw UnsupportedOperationException()

    override fun elicitations(): McpClient.Elicitations = throw UnsupportedOperationException()

    override fun resources() = object : McpClient.Resources {
        override fun onChange(fn: () -> Unit) = throw UnsupportedOperationException()

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send<McpResource.List.Response>(McpResource.List, McpResource.List.Request())
                .map { it.resources }

        override fun listTemplates(overrideDefaultTimeout: Duration?) =
            http.send<McpResource.ListTemplates.Response>(
                McpResource.ListTemplates,
                McpResource.ListTemplates.Request()
            )
                .map { it.resourceTemplates }

        override fun read(
            request: ResourceRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send<McpResource.Read.Response>(McpResource.Read, McpResource.Read.Request(request.uri))
            .map { ResourceResponse(it.contents) }

        override fun subscribe(uri: Uri, fn: () -> Unit) = throw UnsupportedOperationException()

        override fun unsubscribe(uri: Uri) = throw UnsupportedOperationException()
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(ref: Reference, request: CompletionRequest, overrideDefaultTimeout: Duration?) =
            http.send<McpCompletion.Response>(McpCompletion, McpCompletion.Request(ref, request.argument))
                .map { it.completion.run { CompletionResponse(values, total, hasMore) } }
    }

    override fun close() {}

    private inline fun <reified T : ServerMessage> HttpHandler.send(rpc: McpRpc, message: ClientMessage): McpResult<T> {
        val response = this(message.toHttpRequest(protocolVersion, baseUri, rpc).accept(APPLICATION_JSON))

        return when {
            response.status.successful -> Event("message", response.bodyString()).asAOrFailure<T>()

            else -> Failure(Http(response))
        }
    }
}
