import org.http4k.mcp.McpDesktop
import org.http4k.mcp.protocol.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val server = mcpHttp(ServerMetaData(McpEntity("foo", Version.of("bar")))).asServer(Helidon(4444)).start()
    McpDesktop.main("--url", "http://localhost:${server.port()}", "--debug")
}
