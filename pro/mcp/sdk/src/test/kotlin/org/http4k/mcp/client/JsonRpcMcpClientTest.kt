package org.http4k.mcp.client

import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.mcp.server.jsonrpc.JsonRpcClientSessions
import org.http4k.mcp.server.jsonrpc.StandardJsonRpcMcp
import org.http4k.mcp.server.protocol.McpProtocol

class JsonRpcMcpClientTest : McpClientContract<Unit, Response> {

    override val notifications = false

    override fun clientSessions() = JsonRpcClientSessions()

    override fun clientFor(port: Int) = JsonRpcMcpClient(
        Uri.of("http://localhost:${port}/jsonrpc"),
        JavaHttpClient()
    )

    override fun toPolyHandler(protocol: McpProtocol<Unit, Response>) =
        StandardJsonRpcMcp(protocol).debug(debugStream = true)
}
