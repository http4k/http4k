import org.http4k.filter.debug
import org.http4k.mcp.protocol.McpCapability
import org.http4k.mcp.protocol.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val mcpServer = mcpHttp(
        ServerMetaData(
            McpEntity.of("http4k mcp server"), Version.of("0.1.0"),
            *McpCapability.entries.toTypedArray()
        ),
        prompts(),
        resources(),
        tools(),
        sampling()
    )

    mcpServer.debug(debugStream = true).asServer(Helidon(3001)).start()
}
