import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.ServerProtocolCapability
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.filter.debugMcp
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import prompts.argumentsPrompt
import prompts.dynamicPrompt
import prompts.embeddedResourcePrompt
import prompts.emptyCompletion
import prompts.imagePrompt
import prompts.simplePrompt
import resources.dynamicResource
import resources.staticBinaryResource
import resources.staticTextResource
import resources.templateResource
import resources.watchedResource
import tools.audioContentTool
import tools.dynamicTool
import tools.elicitationTool
import tools.elicitationToolSep1034
import tools.embeddedResourceTool
import tools.errorHandlingTool
import tools.imageContentTool
import tools.loggingTool
import tools.multipleContentTypesTool
import tools.progressTool
import tools.samplingTool
import tools.simpleTextTool


fun main() {
    val mcpServer = mcpHttpStreaming(
        ServerMetaData(
            McpEntity.of("http4k MCP conformance"), Version.of("0.1.0"),
            *ServerProtocolCapability.entries.toTypedArray()
        ),
        NoMcpSecurity,
        simpleTextTool(),
        imageContentTool(),
        audioContentTool(),
        embeddedResourceTool(),
        multipleContentTypesTool(),
        progressTool(),
        errorHandlingTool(),
        samplingTool(),
        elicitationTool(),
        elicitationToolSep1034(),
        dynamicTool(),
        loggingTool(),
        staticTextResource(),
        staticBinaryResource(),
        templateResource(),
        watchedResource(),
        dynamicResource(),
        simplePrompt(),
        argumentsPrompt(),
        imagePrompt(),
        embeddedResourcePrompt(),
        dynamicPrompt(),
        emptyCompletion()
    )

    mcpServer.debugMcp().asServer(JettyLoom(4001)).start()
}
