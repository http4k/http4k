package org.http4k.mcp.client

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.client.JavaHttpClient
import org.http4k.client.chunkedSseSequence
import org.http4k.connect.model.ToolName
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.ErrorMessage.Companion.ParseError
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
import org.http4k.lens.accept
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.SamplingHandler
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse.Error
import org.http4k.mcp.ToolResponse.Ok
import org.http4k.mcp.client.McpError.Http
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.protocol.messages.ServerMessage
import org.http4k.mcp.util.McpJson
import org.http4k.sse.SseMessage.Event
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * HTTP Streaming connection MCP client.
 */
class HttpStreamMcpClient(
    private val name: McpEntity,
    private val version: Version,
    private val baseUri: Uri,
    private val http: HttpHandler = JavaHttpClient(responseBodyMode = Stream),
    private val capabilities: ClientCapabilities = ClientCapabilities(),
    private val protocolVersion: ProtocolVersion = LATEST_VERSION
) : McpClient {

    private val sessionId = AtomicReference<SessionId>()

    override fun start() = http.send(
        McpInitialize, McpInitialize.Request(
            VersionedMcpEntity(name, version),
            capabilities,
            protocolVersion
        )
    )
        .flatMap { it.first().asAOrFailure<McpInitialize.Response>() }
        .map { it.also { sessionId.set(it.sessionId) } }
        .map(McpInitialize.Response::capabilities)

    override fun tools() = object : McpClient.Tools {
        override fun onChange(fn: () -> Unit) {

        }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpTool.List, McpTool.List.Request())
                .flatMap { it.first().asAOrFailure<McpTool.List.Response>() }
                .map { it.tools }

        override fun call(
            name: ToolName,
            request: ToolRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send(
            McpTool.Call,
            McpTool.Call.Request(name, request.mapValues { McpJson.asJsonObject(it.value) })
        )
            .flatMap { it.first().asAOrFailure<McpTool.Call.Response>() }

            .map {
                when (it.isError) {
                    true -> Error(ErrorMessage(-1, it.content.joinToString()))
                    else -> Ok(it.content)
                }
            }
    }

    override fun prompts() = object : McpClient.Prompts {
        override fun onChange(fn: () -> Unit) {

        }

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpPrompt.List, McpPrompt.List.Request())
                .flatMap { it.first().asAOrFailure<McpPrompt.List.Response>() }
                .map { it.prompts }

        override fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send(McpPrompt.Get, McpPrompt.Get.Request(name, request))
            .flatMap { it.first().asAOrFailure<McpPrompt.Get.Response>() }
            .map { PromptResponse(it.messages, it.description) }
    }

    override fun sampling() = object : McpClient.Sampling {
        override fun onSampled(overrideDefaultTimeout: Duration?, fn: SamplingHandler) {}
    }

    override fun resources() = object : McpClient.Resources {
        override fun onChange(fn: () -> Unit) {}

        override fun list(overrideDefaultTimeout: Duration?) =
            http.send(McpResource.List, McpResource.List.Request())
                .flatMap { it.first().asAOrFailure<McpResource.List.Response>() }
                .map { it.resources }

        override fun read(
            request: ResourceRequest,
            overrideDefaultTimeout: Duration?
        ) = http.send(McpResource.Read, McpResource.Read.Request(request.uri))
            .flatMap { it.first().asAOrFailure<McpResource.Read.Response>() }
            .map { ResourceResponse(it.contents) }

        override fun subscribe(uri: Uri, fn: () -> Unit) {}

        override fun unsubscribe(uri: Uri) {}
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(request: CompletionRequest, overrideDefaultTimeout: Duration?) =
            http.send(McpCompletion, McpCompletion.Request(request.ref, request.argument))
                .flatMap { it.first().asAOrFailure<McpCompletion.Response>() }
                .map { it.completion.run { CompletionResponse(values, total, hasMore) } }
    }

    override fun close() {}

    private fun HttpHandler.send(rpc: McpRpc, message: ClientMessage): McpResult<Sequence<Event>> {
        val response = this(
            message.toHttpRequest(baseUri, rpc)
                .accept(TEXT_EVENT_STREAM)
                .with(Header.MCP_SESSION_ID of sessionId.get())
        )

        return when {
            response.status.successful -> {
                sessionId.set(Header.MCP_SESSION_ID(response))

                Success(response.body.stream.chunkedSseSequence().filterIsInstance<Event>())
            }

            else -> Failure(Http(response))
        }
    }
}

private inline fun <reified T : ServerMessage> Event.asAOrFailure(): Result<T, McpError.Protocol> = with(McpJson) {
    val data = parse(data) as MoshiObject

    when {
        data["method"] != null -> Failure(McpError.Protocol(InvalidRequest))
        else -> {
            resultFrom {
                convert<MoshiNode, T>(data.attributes["result"] ?: nullNode())
            }.mapFailure { McpError.Protocol(ParseError) }
        }
    }
}
