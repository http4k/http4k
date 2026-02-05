package org.http4k.ai.mcp.client.sse

import org.http4k.ai.mcp.client.McpStreamingClientContract
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.ApiKeyMcpSecurity
import org.http4k.ai.mcp.server.sse.SseMcp
import org.http4k.ai.mcp.server.sse.SseSessions
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header
import org.http4k.sse.Sse

class SseMcpClientTest : McpStreamingClientContract<Sse>() {

    override val doesNotifications = true

    override fun clientFor(port: Int) = SseMcpClient(
        clientName, Version.of("1.0.0"),
        Request(GET, Uri.of("http://localhost:${port}/sse")),
        ClientFilters.ApiKeyAuth(Header.required("KEY") of "123").then(JavaHttpClient(responseBodyMode = Stream)),
        ClientCapabilities()
    )

    override fun clientSessions() = SseSessions().apply { start() }

    override fun toPolyHandler(protocol: McpProtocol<Sse>) =
        SseMcp(protocol, ApiKeyMcpSecurity(Header.required("KEY"), { it == "123" }))
}
