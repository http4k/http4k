//package org.http4k.mcp.testing.capabilities
//
//import dev.forkhandles.result4k.map
//import org.http4k.connect.model.ToolName
//import org.http4k.jsonrpc.ErrorMessage
//import org.http4k.mcp.ToolRequest
//import org.http4k.mcp.ToolResponse
//import org.http4k.mcp.client.McpClient
//import org.http4k.mcp.client.McpResult
//import org.http4k.mcp.model.Content
//import org.http4k.mcp.model.Meta
//import org.http4k.mcp.protocol.messages.McpTool
//import org.http4k.mcp.testing.TestMcpSender
//import org.http4k.mcp.testing.nextEvent
//import org.http4k.mcp.testing.nextNotification
//import org.http4k.mcp.util.McpJson
//import org.http4k.testing.TestSseClient
//import java.time.Duration
//import java.util.concurrent.atomic.AtomicReference
//
//class TestMcpClientTools(private val sender: TestMcpSender, private val client: AtomicReference<TestSseClient>) :
//    McpClient.Tools {
//
//    private val notifications = mutableListOf<() -> Unit>()
//
//    override fun onChange(fn: () -> Unit) {
//        notifications += fn
//    }
//
//    /**
//     * Force a list changed notification to be received and process it
//     */
//    fun expectNotification() =
//        client.nextNotification<McpTool.List.Changed.Notification>(McpTool.List.Changed)
//            .also { notifications.forEach { it() } }
//
//    override fun list(overrideDefaultTimeout: Duration?): McpResult<List<McpTool>> {
//        sender(McpTool.List, McpTool.List.Request())
//        return client.nextEvent<McpTool.List.Response, List<McpTool>> { tools }.map { it.second }
//    }
//
//    override fun call(
//        name: ToolName,
//        request: ToolRequest,
//        overrideDefaultTimeout: Duration?
//    ): McpResult<ToolResponse> {
//        sender(
//            McpTool.Call, McpTool.Call.Request(
//                name,
//                request.mapValues { McpJson.asJsonObject(it.value) }, Meta(request.progressToken)
//            )
//        )
//        return client.nextEvent<McpTool.Call.Response, ToolResponse>({
//            when (isError) {
//                true -> {
//                    val input = (content.first() as Content.Text).text
//                    ToolResponse.Error(McpJson.asA<ErrorMessage>(input))
//                }
//
//                else -> ToolResponse.Ok(content)
//            }
//        }).map { it.second }
//    }
//}
