package org.http4k.mcp.client

import org.http4k.client.WebsocketClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.server.McpWsHandler
import org.http4k.mcp.server.sse.RealtimeMcpProtocol
import org.http4k.server.Helidon
import org.http4k.server.asServer
import java.time.Duration

class WsMcpClientTest : McpClientContract {

    override fun clientFor(port: Int) = WsMcpClient(
        Request(GET, Uri.of("ws://localhost:${port}/ws")),
        WebsocketClient(Duration.ofSeconds(2), true),
        VersionedMcpEntity(McpEntity.of("foobar"), Version.of("1.0.0")),
        ClientCapabilities(),
    )

    override fun toPolyHandler(protocol: RealtimeMcpProtocol) = McpWsHandler(protocol).asServer(Helidon(0))
}
