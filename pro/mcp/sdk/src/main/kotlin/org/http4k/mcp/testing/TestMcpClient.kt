package org.http4k.mcp.testing

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.format.renderRequest
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcResult
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
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpClient.Completions
import org.http4k.mcp.client.McpClient.Prompts
import org.http4k.mcp.client.McpClient.Resources
import org.http4k.mcp.client.McpClient.Tools
import org.http4k.mcp.client.McpError
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.ToolName
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolCapability
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-10-07`
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.ClientMessage
import org.http4k.mcp.protocol.messages.McpCompletion
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpRpc
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType
import org.http4k.sse.SseMessage
import org.http4k.testing.TestSseClient
import org.http4k.testing.testSseClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * Create an in-memory MCP test client
 */
fun PolyHandler.testMcpClient(connectRequest: Request = Request(GET, "/sse")) = object : McpClient {

    private val messageRequest = AtomicReference<Request>()

    private val client = AtomicReference<TestSseClient>()

    override fun start(): McpResult<ServerCapabilities> {
        val mcpResponse = sse!!.testSseClient(connectRequest)

        client.set(mcpResponse)

        require(mcpResponse.status == OK)

        val endpointEvent = mcpResponse.received().first() as SseMessage.Event

        require(endpointEvent.event == "endpoint") { "no endpoint event" }
        messageRequest.set(Request(POST, Uri.of(endpointEvent.data)))
        sendToMcp(
            McpInitialize, McpInitialize.Request(
                VersionedMcpEntity(McpEntity.of("client"), Version.of("1")),
                ClientCapabilities(*ProtocolCapability.entries.toTypedArray<ProtocolCapability>()), `2024-10-07`
            )
        )
        sendToMcp(McpInitialize.Initialized, McpInitialize.Initialized.Notification)

        return nextEvent<McpInitialize.Response, ServerCapabilities> { capabilities }
    }

    override fun tools() = object : Tools {
        override fun onChange(fn: () -> Unit) {
            TODO()
        }

        override fun list(overrideDefaultTimeout: Duration?): McpResult<List<McpTool>> {
            sendToMcp(McpTool.List, McpTool.List.Request())
            return nextEvent<McpTool.List.Response, List<McpTool>> { tools }
        }

        override fun call(
            name: ToolName,
            request: ToolRequest,
            overrideDefaultTimeout: Duration?
        ): McpResult<ToolResponse> {
            sendToMcp(
                McpTool.Call, McpTool.Call.Request(
                    name,
                    request.mapValues { McpJson.asJsonObject(it.value) })
            )
            return nextEvent<McpTool.Call.Response, ToolResponse>() {
                when (isError) {
                    true -> ToolResponse.Error(ErrorMessage.InternalError)
                    else -> ToolResponse.Ok(content)
                }
            }
        }
    }

    override fun prompts() = object : Prompts {
        override fun onChange(fn: () -> Unit) {
            TODO()
        }

        override fun list(overrideDefaultTimeout: Duration?): McpResult<List<McpPrompt>> {
            sendToMcp(McpPrompt.List, McpPrompt.List.Request())
            return nextEvent<McpPrompt.List.Response, List<McpPrompt>> { prompts }
        }

        override fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration?
        ): McpResult<PromptResponse> {
            sendToMcp(McpPrompt.Get, McpPrompt.Get.Request(name, request))
            return nextEvent<McpPrompt.Get.Response, PromptResponse> {
                PromptResponse(messages, description)
            }
        }
    }

    override fun sampling() = object : McpClient.Sampling {
        override fun sample(
            name: ModelIdentifier,
            request: SamplingRequest,
            fetchNextTimeout: Duration?
        ): Sequence<McpResult<SamplingResponse>> {
            TODO("Not yet implemented")
        }
    }

    override fun resources() = object : Resources {
        override fun onChange(fn: () -> Unit) {
            TODO()
        }

        override fun list(overrideDefaultTimeout: Duration?): McpResult<List<McpResource>> {
            sendToMcp(McpResource.List, McpResource.List.Request())
            return nextEvent<McpResource.List.Response, List<McpResource>> { resources }
        }

        override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?): McpResult<ResourceResponse> {
            sendToMcp(McpResource.Read, McpResource.Read.Request(request.uri))
            return nextEvent<McpResource.Read.Response, ResourceResponse> { ResourceResponse(contents) }
        }
    }

    override fun completions() = object : Completions {
        override fun complete(
            request: CompletionRequest,
            overrideDefaultTimeout: Duration?
        ): McpResult<CompletionResponse> {
            sendToMcp(McpCompletion, McpCompletion.Request(request.ref, request.argument))
            return nextEvent<McpCompletion.Response, CompletionResponse> { CompletionResponse(completion) }
        }
    }

    override fun close() {
    }

    private fun sendToMcp(hasMethod: McpRpc, input: ClientMessage.Request) {
        sendToMcp(with(McpJson) {
            compact(renderRequest(hasMethod.Method.value, asJsonObject(input), number(1)))
        })
    }

    private inline fun <reified T : Any, OUT> nextEvent(fn: T.() -> OUT): McpResult<OUT> {
        val jsonRpcResult = JsonRpcResult(
            McpJson,
            McpJson.fields(McpJson.parse((client.get().received().first() as SseMessage.Event).data)).toMap()
        )

        return when {
            jsonRpcResult.isError() -> Failure(
                McpError.Protocol(
                    McpJson.convert<McpNodeType, ErrorMessage>(
                        jsonRpcResult.error!!
                    )
                )
            )

            else -> Success(fn(McpJson.convert<McpNodeType, T>(jsonRpcResult.result!!)))
        }
    }

    private fun sendToMcp(hasMethod: McpRpc, input: ClientMessage.Notification) {
        sendToMcp(with(McpJson) {
            compact(renderRequest(hasMethod.Method.value, asJsonObject(input), number(1)))
        })
    }

    private fun sendToMcp(body: String) {
        require(http!!(messageRequest.get().body(body)).status == ACCEPTED)
    }
}
