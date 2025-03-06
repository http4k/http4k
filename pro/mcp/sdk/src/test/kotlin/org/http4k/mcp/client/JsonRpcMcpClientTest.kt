package org.http4k.mcp.client

import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.jsonrpc.JsonRpcTransport
import org.http4k.mcp.server.jsonrpc.StandardJsonRpcMcp
import org.http4k.mcp.server.protocol.McpProtocol

class JsonRpcMcpClientTest : McpClientContract<Response, McpProtocol<Unit, Response>> {

    override val notifications = false

    override fun protocol(
        serverMetaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions
    ) = McpProtocol(JsonRpcTransport(), serverMetaData, tools, resources, prompts, completions)

    override fun clientFor(port: Int) = JsonRpcMcpClient(
        Uri.of("http://localhost:${port}/message"),
        JavaHttpClient()
    )

    override fun toPolyHandler(protocol: McpProtocol<Unit, Response>) = StandardJsonRpcMcp(protocol)
}
