package org.http4k.ai.mcp.conformance.server

import org.http4k.ai.mcp.conformance.server.prompts.argumentsPrompt
import org.http4k.ai.mcp.conformance.server.prompts.dynamicPrompt
import org.http4k.ai.mcp.conformance.server.prompts.embeddedResourcePrompt
import org.http4k.ai.mcp.conformance.server.prompts.emptyCompletion
import org.http4k.ai.mcp.conformance.server.prompts.imagePrompt
import org.http4k.ai.mcp.conformance.server.prompts.simplePrompt
import org.http4k.ai.mcp.conformance.server.resources.dynamicResource
import org.http4k.ai.mcp.conformance.server.resources.staticBinaryResource
import org.http4k.ai.mcp.conformance.server.resources.staticTextResource
import org.http4k.ai.mcp.conformance.server.resources.templateResource
import org.http4k.ai.mcp.conformance.server.resources.watchedResource
import org.http4k.ai.mcp.conformance.server.tools.audioContentTool
import org.http4k.ai.mcp.conformance.server.tools.dynamicTool
import org.http4k.ai.mcp.conformance.server.tools.elicitationTool
import org.http4k.ai.mcp.conformance.server.tools.elicitationToolSep1034
import org.http4k.ai.mcp.conformance.server.tools.embeddedResourceTool
import org.http4k.ai.mcp.conformance.server.tools.errorHandlingTool
import org.http4k.ai.mcp.conformance.server.tools.imageContentTool
import org.http4k.ai.mcp.conformance.server.tools.loggingTool
import org.http4k.ai.mcp.conformance.server.tools.multipleContentTypesTool
import org.http4k.ai.mcp.conformance.server.tools.progressTool
import org.http4k.ai.mcp.conformance.server.tools.samplingTool
import org.http4k.ai.mcp.conformance.server.tools.simpleTextTool
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.ServerProtocolCapability
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.filter.debugMcp
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

fun McpConformanceServer() = mcpHttpStreaming(
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

fun main() {
    McpConformanceServer().debugMcp().asServer(JettyLoom(4001)).start()
}

