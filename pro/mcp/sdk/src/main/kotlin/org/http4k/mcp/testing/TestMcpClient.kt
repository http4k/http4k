package org.http4k.mcp.testing

import dev.forkhandles.result4k.map
import org.http4k.connect.model.ToolName
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.CompletionRequest
import org.http4k.mcp.CompletionResponse
import org.http4k.mcp.PromptRequest
import org.http4k.mcp.PromptResponse
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.client.McpResult
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ProtocolVersion.Companion.`2024-11-05`
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
import org.http4k.mcp.testing.capabilities.TestMcpClientRequestProgress
import org.http4k.mcp.testing.capabilities.TestMcpClientSampling
import org.http4k.mcp.testing.capabilities.TestMcpClientTools
import org.http4k.mcp.util.McpJson
import org.http4k.testing.TestSseClient
import org.http4k.testing.testSseClient
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Create an in-memory MCP test client - HTTP Streaming only
 */
fun PolyHandler.testMcpClient(connectRequest: Request = Request(POST, "/mcp")) = TestMcpClient(this, connectRequest)

class TestMcpClient(private val poly: PolyHandler, private val connectRequest: Request) : McpClient {

//    private val tools = TestMcpClientTools(send, client)
//    private val prompts = TestMcpClientPrompts(send, client)
//    private val progress = TestMcpClientRequestProgress(client)
//    private val sampling = TestMcpClientSampling(send, client)
//    private val resources = TestMcpClientResources(send, client)
//    private val completions = TestMcpClientCompletions(send, client)

    private var id = AtomicInteger(0)

    override fun start(): McpResult<ServerCapabilities> {
        val initResponse = send(
            McpInitialize, McpInitialize.Request(
                VersionedMcpEntity(McpEntity.of("client"), Version.of("1")),
                ClientCapabilities(), `2024-11-05`
            )
        )

        send(McpInitialize.Initialized, McpInitialize.Initialized.Notification).received().toList()
        return initResponse.nextEvent<McpInitialize.Response, ServerCapabilities> { capabilities }.map { it.second }
    }

    override fun tools(): McpClient.Tools = object : McpClient.Tools {
        override fun onChange(fn: () -> Unit) {
        }

        override fun list(overrideDefaultTimeout: Duration?) =
            send(McpTool.List, McpTool.List.Request()).nextEvent<McpTool.List.Response, List<McpTool>> { tools }
                .map { it.second }

        override fun call(
            name: ToolName,
            request: ToolRequest,
            overrideDefaultTimeout: Duration?
        ) = send(
            McpTool.Call, McpTool.Call.Request(
                name,
                request.mapValues { McpJson.asJsonObject(it.value) }, Meta(request.progressToken)
            )
        ).nextEvent<McpTool.Call.Response, ToolResponse>({
            when (isError) {
                true -> {
                    val input = (content.first() as Content.Text).text
                    ToolResponse.Error(McpJson.asA<ErrorMessage>(input))
                }

                else -> ToolResponse.Ok(content)
            }
        }).map { it.second }
    }

    override fun prompts() = object : McpClient.Prompts {
        private val notifications = mutableListOf<() -> Unit>()

        override fun onChange(fn: () -> Unit) {
            notifications += fn
        }
//
//        /**
//         * Expected a list changed notification to be received and process it
//         */
//        fun expectNotification() =
//            client.nextNotification<McpPrompt.List.Changed.Notification>(McpPrompt.List.Changed)
//                .also { notifications.forEach { it() } }

        override fun list(overrideDefaultTimeout: Duration?): McpResult<List<McpPrompt>> =
            send(
                McpPrompt.List,
                McpPrompt.List.Request()
            ).nextEvent<McpPrompt.List.Response, List<McpPrompt>> { prompts }.map { it.second }

        override fun get(
            name: PromptName,
            request: PromptRequest,
            overrideDefaultTimeout: Duration?
        ): McpResult<PromptResponse> {
            return send(
                McpPrompt.Get,
                McpPrompt.Get.Request(name, request)
            ).nextEvent<McpPrompt.Get.Response, PromptResponse>({
                PromptResponse(messages, description)
            }).map { it.second }
        }
    }

    override fun progress(): TestMcpClientRequestProgress = TODO()

    override fun sampling(): TestMcpClientSampling = TODO()

    override fun resources() = object : McpClient.Resources {

        override fun onChange(fn: () -> Unit) {
        }

        override fun list(overrideDefaultTimeout: Duration?) =
            send(
                McpResource.List,
                McpResource.List.Request()
            ).nextEvent<McpResource.List.Response, List<McpResource>> { resources }.map { it.second }

        override fun read(request: ResourceRequest, overrideDefaultTimeout: Duration?) =
            send(
                McpResource.Read,
                McpResource.Read.Request(request.uri)
            ).nextEvent<McpResource.Read.Response, ResourceResponse> { ResourceResponse(contents) }
                .map { it.second }


        override fun subscribe(uri: Uri, fn: () -> Unit) {
            send(McpResource.Subscribe, McpResource.Subscribe.Request(uri))
//            subscriptions.getOrPut(uri, ::mutableListOf).add(fn)
        }

        override fun unsubscribe(uri: Uri) {
            send(McpResource.Unsubscribe, McpResource.Unsubscribe.Request(uri))
//            subscriptions -= uri
        }
    }

    override fun completions() = object : McpClient.Completions {
        override fun complete(
            request: CompletionRequest,
            overrideDefaultTimeout: Duration?
        ) = send(McpCompletion, McpCompletion.Request(request.ref, request.argument))
            .nextEvent<McpCompletion.Response, CompletionResponse>(
                { CompletionResponse(completion.values, completion.total, completion.hasMore) }
            ).map { it.second }
    }

    override fun close() {
    }

    private fun send(mcpRpc: McpRpc, input: ClientMessage.Request): TestSseClient {
        val client = poly.testSseClient(
            connectRequest.withMcp(mcpRpc, input, id.incrementAndGet())
        )

        require(client.status == OK)
        return client
    }

    private fun send(mcpRpc: McpRpc, input: ClientMessage.Notification): TestSseClient {
        val client = poly.testSseClient(
            connectRequest.withMcp(mcpRpc, input, id.incrementAndGet())
        )

        require(client.status == OK)
        return client
    }
}

