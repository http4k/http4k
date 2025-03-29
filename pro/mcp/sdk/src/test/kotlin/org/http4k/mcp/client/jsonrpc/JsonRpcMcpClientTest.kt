package org.http4k.mcp.client.jsonrpc

import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.mcp.client.McpClientContract
import org.http4k.mcp.server.jsonrpc.JsonRpcSessions
import org.http4k.mcp.server.jsonrpc.JsonRpcMcp
import org.http4k.mcp.server.protocol.McpProtocol

class JsonRpcMcpClientTest : McpClientContract<Unit> {

    override val doesNotifications = false

    override fun clientSessions() = JsonRpcSessions()

    override fun clientFor(port: Int) = JsonRpcMcpClient(
        Uri.of("http://localhost:${port}/jsonrpc"),
        JavaHttpClient()
    )

    override fun toPolyHandler(protocol: McpProtocol<Unit>) = JsonRpcMcp(protocol)
}
