package security

import org.http4k.base64Encode
import org.http4k.core.Credentials
import org.http4k.filter.debug
import org.http4k.mcp.McpDesktop
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpSse
import org.http4k.security.BasicAuthSecurity
import org.http4k.security.then
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val insecureMcpServer = mcpSse(ServerMetaData(McpEntity.of("foo"), Version.of("bar")))

    val secureMcpServer = BasicAuthSecurity("", Credentials("foo", "bar"))
        .then(insecureMcpServer)

    secureMcpServer.debug(System.err, true).asServer(Helidon(3001)).start()
    McpDesktop.main(
        "--url", "http://localhost:3001/sse",
        "--basicAuth", "foo:bar"
    )
}
