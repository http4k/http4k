package org.http4k.mcp.client

import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.server.McpSseHandler
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.IncomingSampling
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.sse.RealtimeMcpProtocol
import org.http4k.server.Helidon
import org.http4k.server.asServer

class SseMcpClientTest : McpClientContract<Response, RealtimeMcpProtocol> {
    override fun protocol(
        serverMetaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions,
        incomingSampling: IncomingSampling
    ) = RealtimeMcpProtocol(serverMetaData, prompts, tools, resources, completions, incomingSampling)

    override fun clientFor(port: Int) = SseMcpClient(
        Request(GET, Uri.of("http://localhost:${port}/sse")),
        VersionedMcpEntity(McpEntity.of("foobar"), Version.of("1.0.0")),
        ClientCapabilities(),
        JavaHttpClient(responseBodyMode = Stream)
    )

    override fun toPolyHandler(protocol: RealtimeMcpProtocol) = McpSseHandler(protocol).asServer(Helidon(0))
}
