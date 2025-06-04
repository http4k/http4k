package file

import org.http4k.client.ReconnectionMode.Disconnect
import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Uri
import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Resource.Content
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import java.io.File

fun main() {

    fun dir(root: File): ResourceHandler = {
        ResourceResponse(
            Content.Text(
                File(root, it.uri.toString().removePrefix("home://")).readText(),
                it.uri,
                MimeType.of(TEXT_PLAIN)
            )
        )
    }

    val server =
        mcpHttpStreaming(
            ServerMetaData("http4k_mcp", "1"),
            NoMcpSecurity,
            Resource.Templated("home://{path}", "foo") bind dir(File("."))
        ).asServer(JettyLoom(8000)).start()

    Thread.sleep(200)

    HttpStreamingMcpClient(
        McpEntity.of("client"), Version.of("1"),
        Uri.of("http://localhost:8000/mcp"),
        notificationSseReconnectionMode = Disconnect
    ).use {
        it.start()
        println(it.resources().read(ResourceRequest(Uri.of("home://bob.txt"))))
    }
}

