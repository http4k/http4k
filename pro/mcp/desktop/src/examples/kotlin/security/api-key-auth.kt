package security

import org.http4k.filter.debug
import org.http4k.lens.Header
import org.http4k.mcp.McpDesktop
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpSse
import org.http4k.security.ApiKeySecurity
import org.http4k.security.then
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val insecureMcpServer = mcpSse(ServerMetaData(McpEntity.of("foo"), Version.of("bar")))

    val secureMcpServer = ApiKeySecurity(Header.required("X-API-key"), { it == "foobar" })
        .then(insecureMcpServer)

    secureMcpServer.debug(System.err, true).asServer(Helidon(3001)).start()

    McpDesktop.main(
        "--url", "http://localhost:3001/sse",
        "--apiKey", "foobar"
    )
}
