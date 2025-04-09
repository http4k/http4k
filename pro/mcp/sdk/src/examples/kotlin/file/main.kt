package file

import org.http4k.client.ReconnectionMode.Disconnect
import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Uri
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceRequest
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.client.http.HttpStreamingMcpClient
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Resource
import org.http4k.mcp.model.Resource.Content
import org.http4k.mcp.model.Resource.Static
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.Helidon
import org.http4k.server.asServer
import java.io.File
import java.net.URI
import java.nio.file.Paths
import kotlin.io.path.exists

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
            Resource.Templated("home://{path}", "foo") bind dir(File("."))
        ).asServer(Helidon(8000)).start()

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

