import org.http4k.filter.debug
import org.http4k.mcp.protocol.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpHttp
import org.http4k.server.Helidon
import org.http4k.server.asServer

fun main() {
    val mcpServer = mcpHttp(
        ServerMetaData(McpEntity("http4k mcp server", Version.of("0.1.0"))),
        prompt1(),
        prompt2(),
        staticResource(),
        templatedResource(),
        liveWeatherTool(),
        reverseTool(),
        countingTool(),
        llm(),
        sampleFromModel()
    )

    mcpServer.debug(debugStream = true).asServer(Helidon(3001)).start()
}
