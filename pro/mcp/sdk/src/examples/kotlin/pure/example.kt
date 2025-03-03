package pure

import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.format.renderRequest
import org.http4k.lens.accept
import org.http4k.lens.contentType
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.RequestId
import org.http4k.mcp.protocol.ProtocolCapability
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.util.McpJson
import org.http4k.routing.mcpPureSse
import org.http4k.server.Helidon
import org.http4k.server.asServer
import server.completions
import server.prompts
import server.resources
import server.sampling
import server.tools

fun main() {
    val mcpServer = mcpPureSse(
        ServerMetaData(
            McpEntity.of("http4k mcp server"), Version.of("0.1.0"),
            *ProtocolCapability.entries.toTypedArray()
        ),
        prompts(),
        resources(),
        tools(),
        sampling(),
        completions()
    )

    mcpServer.asServer(Helidon(3001)).start().use {
        val request = Request(POST, "http://localhost:3001/sse")
            .contentType(APPLICATION_JSON)
            .accept(TEXT_EVENT_STREAM)
            .body(with(McpJson) {
                compact(
                    renderRequest(
                        McpTool.List.Method.value,
                        asJsonObject(McpTool.List.Request()),
                        McpJson.asJsonObject(RequestId.random())
                    )
                )
            })

        println(JavaHttpClient(responseBodyMode = Stream)(request))
    }
}

