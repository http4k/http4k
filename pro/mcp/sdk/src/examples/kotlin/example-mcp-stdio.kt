import org.http4k.mcp.protocol.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.routing.mcpStdIo

fun main() {
    mcpStdIo(
        ServerMetaData(McpEntity("http4k mcp stdio", Version.of("0.1.0"))),
        prompt1(),
        prompt2(),
        staticResource(),
        templatedResource(),
        liveWeatherTool(),
        reverseTool(),
        countingTool(),
        llm()
    )
}
