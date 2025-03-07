package org.http4k.mcp.client

import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.jsonrpc.JsonRpcClientSessions
import org.http4k.mcp.server.jsonrpc.StandardJsonRpcMcp
import org.http4k.mcp.server.protocol.Completions
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Prompts
import org.http4k.mcp.server.protocol.Resources
import org.http4k.mcp.server.protocol.Tools

class JsonRpcMcpClientTest : McpClientContract<Response, McpProtocol<Unit, Response>> {

    override val notifications = false

    override fun protocol(
        serverMetaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions
    ) = McpProtocol(serverMetaData, JsonRpcClientSessions(), tools, resources, prompts, completions)

    override fun clientFor(port: Int) = JsonRpcMcpClient(
        Uri.of("http://localhost:${port}/jsonrpc"),
        JavaHttpClient()
    )

    override fun toPolyHandler(protocol: McpProtocol<Unit, Response>) =
        StandardJsonRpcMcp(protocol).debug(debugStream = true)
}
