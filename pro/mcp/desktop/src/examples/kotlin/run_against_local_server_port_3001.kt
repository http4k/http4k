import org.http4k.filter.debug
import org.http4k.mcp.McpDesktop
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val server = mcpHttp(ServerMetaData(McpEntity.of("foo"), Version.of("bar"))).debug(System.err, true).asServer(Helidon(3001)).start()
    McpDesktop.main("--url", "http://localhost:${server.port()}")
}
