package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.McpCapability.Experimental
import org.http4k.mcp.protocol.McpCapability.Logging
import org.http4k.mcp.protocol.McpCapability.PromptsChanged
import org.http4k.mcp.protocol.McpCapability.ResourcesChanged
import org.http4k.mcp.protocol.McpCapability.Sampling
import org.http4k.mcp.protocol.McpCapability.ToolsChanged
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ServerCapabilities internal constructor(
    val tools: ToolCapabilities = ToolCapabilities(),
    val prompts: PromptCapabilities = PromptCapabilities(),
    val resources: ResourceCapabilities = ResourceCapabilities(),
    val experimental: Unit? = null,
    val logging: Unit? = null,
    val sampling: Unit? = null,
) {
    constructor(vararg capabilities: McpCapability) : this(
        ToolCapabilities(capabilities.contains(ToolsChanged)),
        PromptCapabilities(capabilities.contains(PromptsChanged)),
        ResourceCapabilities(capabilities.contains(ResourcesChanged)),
        if (capabilities.contains(Experimental)) Unit else null,
        if (capabilities.contains(Logging)) Unit else null,
        if (capabilities.contains(Sampling)) Unit else null,
    )

    @JsonSerializable
    data class ToolCapabilities(val listChanged: Boolean? = false)

    @JsonSerializable
    data class PromptCapabilities(val listChanged: Boolean = false)

    @JsonSerializable
    data class ResourceCapabilities(val subscribe: Boolean? = false, val listChanged: Boolean? = false)
}

