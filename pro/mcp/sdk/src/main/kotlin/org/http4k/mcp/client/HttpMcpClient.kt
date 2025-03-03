package org.http4k.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.client.JavaHttpClient
import org.http4k.client.chunkedSseSequence
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.ParseError
import org.http4k.lens.accept
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingHandler
import org.http4k.mcp.SamplingRequest
import org.http4k.mcp.SamplingResponse
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse.Error
import org.http4k.mcp.ToolResponse.Ok
import org.http4k.mcp.client.McpError.Http
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.protocol.messages.McpSampling
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.protocol.messages.ServerMessage
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage.Event
import java.time.Duration

/**
 * HTTP connection MCP client.
 */
class HttpMcpClient(
    private val uri: Uri,
    private val http: HttpHandler = JavaHttpClient()
) : McpClient {
    override fun start() = Success(ServerCapabilities())

    override fun tools() = object : McpClient.Tools {
        override fun onChange(fn: () -> Unit) = throw UnsupportedOperationException()

        override fun list(overrideDefaultTimeout: Duration?) =
            send<McpTool.List.Response>(McpTool.List, McpTool.List.Request())
                .flatMap { it.first().map { it.tools } }

        override fun call(
            name: ToolName,
            request: ToolRequest,
            overrideDefaultTimeout: Duration?
        ) = send<McpTool.Call.Response>(
            McpTool.Call,
            McpTool.Call.Request(name, request.mapValues { McpJson.asJsonObject(it.value) })
        )
            .flatMap { it.first() }
            .map {
                when (it.isError) {
                    true -> Error(ErrorMessage(-1, it.content.joinToString()))
                    else -> Ok(it.content)
                }
            }
    }

    override fun prompts() = object : McpClient.Prompts {
        override fun onChange(fn: () -> Unit) = throw UnsupportedOperationException()

        override fun list(overrideDefaultTimeout: Duration?) =
            send<McpPrompt.List.Response>(McpPrompt.List, McpPrompt.List.Request())
                .flatMap { it.first().map { it.prompts } }

        override fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration?
        ) = send<McpPrompt.Get.Response>(McpPrompt.Get, McpPrompt.Get.Request(name, request))
            .flatMap { it.first().map { PromptResponse(it.messages, it.description) } }
    }

    override fun sampling() = object : McpClient.Sampling {
        override fun sample(
            name: ModelIdentifier,
            request: SamplingRequest,
            fetchNextTimeout: Duration?
        ) = send<McpSampling.Response>(
            McpSampling,
            with(request) {
                McpSampling.Request(
                    messages,
                    maxTokens,
                    systemPrompt,
                    includeContext,
                    temperature,
                    stopSequences,
                    modelPreferences,
                    metadata
                )
            }
        )
            .map { it.map { it.map { SamplingResponse(it.model, it.role, it.content, it.stopReason) } } }
            .onFailure { return listOf(Failure(it.reason)).asSequence() }

        override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) =
            throw UnsupportedOperationException()
    }

    override fun resources() = object : McpClient.Resources {
        override fun onChange(fn: () -> Unit) = throw UnsupportedOperationException()

        override fun list(overrideDefaultTimeout: Duration?) =
            send<McpResource.List.Response>(McpResource.List, McpResource.List.Request())
                .flatMap { it.first().map { it.resources } }

        override fun read(
            request: ResourceRequest,
            overrideDefaultTimeout: Duration?
        ) = send<McpResource.Read.Response>(McpResource.Read, McpResource.Read.Request(request.uri))
            .flatMap { it.first().map { ResourceResponse(it.contents) } }

        override fun subscribe(uri: Uri, fn: () -> Unit) = throw UnsupportedOperationException()

        override fun unsubscribe(uri: Uri) = throw UnsupportedOperationException()
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(
            request: CompletionRequest,
            overrideDefaultTimeout: Duration?
        ) = send<McpCompletion.Response>(McpCompletion, McpCompletion.Request(request.ref, request.argument))
            .flatMap { it.first().map { CompletionResponse(it.completion) } }
    }

    override fun close() {
    }

    private inline fun <reified T : ServerMessage> send(list: McpRpc, msg: ClientMessage)
        : McpResult<Sequence<McpResult<T>>> {
        val response = http(msg.toHttpRequest(uri, list).accept(TEXT_EVENT_STREAM))
        return when {
            response.status.successful ->
                Success(response.body.stream.chunkedSseSequence().mapNotNull {
                    when (it) {
                        is Event ->
                            with(McpJson) {
                                val data = parse(it.data) as MoshiObject

                                when {
                                    data["method"] != null -> null
                                    else -> {
                                        resultFrom {
                                            McpJson.convert<MoshiNode, T>(data.attributes["result"] ?: nullNode())
                                        }.mapFailure { McpError.Protocol(ParseError) }
                                    }
                                }
                            }

                        else -> null
                    }
                })

            else -> Failure(Http(response))
        }
    }
}
