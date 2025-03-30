package org.http4k.mcp.testing

import dev.forkhandles.result4k.map
import org.http4k.connect.model.ToolName
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.client.McpClient
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.util.McpJson
import java.time.Duration

class TestingTools(private val send: TestMcpSender) : McpClient.Tools {
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
