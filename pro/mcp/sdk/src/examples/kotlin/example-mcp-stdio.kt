import org.http4k.mcp.protocol.McpCapability
import org.http4k.mcp.protocol.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.stdio.DebuggingReader
import org.http4k.mcp.stdio.DebuggingWriter
import org.http4k.routing.mcpStdIo

fun main() {
    mcpStdIo(
        ServerMetaData(
            McpEntity.of("http4k mcp stdio"), Version.of("0.1.0"),
            *McpCapability.entries.toTypedArray()
        ),
        prompt1(),
        prompt2(),
        staticResource(),
        templatedResource(),
        liveWeatherTool(),
        reverseTool(),
        countingTool(),
        llm(),
        reader = DebuggingReader(System.`in`.reader()),
        writer = DebuggingWriter(System.out.writer())
    )
}
