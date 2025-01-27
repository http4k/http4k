import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ProtocolCapability
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.util.DebuggingReader
import org.http4k.mcp.util.DebuggingWriter
import org.http4k.routing.mcpStdIo

fun main() {
    mcpStdIo(
        ServerMetaData(
            McpEntity.of("http4k mcp stdio"), Version.of("0.1.0"),
            *ProtocolCapability.entries.toTypedArray()
        ),
        prompts(),
        resources(),
        tools(),
        sampling(),
        reader = DebuggingReader(System.`in`.reader()),
        writer = DebuggingWriter(System.out.writer())
    )
}
