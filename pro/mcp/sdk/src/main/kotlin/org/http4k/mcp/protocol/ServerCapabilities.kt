package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.ProtocolCapability.Experimental
import org.http4k.mcp.protocol.ProtocolCapability.Logging
import org.http4k.mcp.protocol.ProtocolCapability.PromptsChanged
import org.http4k.mcp.protocol.ProtocolCapability.ResourcesChanged
import org.http4k.mcp.protocol.ProtocolCapability.Completions
import org.http4k.mcp.protocol.ProtocolCapability.ToolsChanged
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
@ConsistentCopyVisibility
data class ServerCapabilities internal constructor(
    val tools: ToolCapabilities = ToolCapabilities(),
    val prompts: PromptCapabilities = PromptCapabilities(),
    val resources: ResourceCapabilities = ResourceCapabilities(),
    val experimental: Unit? = null,
    val completions: Unit? = null,
    val logging: Unit? = null
) {
    constructor(vararg capabilities: ProtocolCapability) : this(
        ToolCapabilities(capabilities.contains(ToolsChanged)),
        PromptCapabilities(capabilities.contains(PromptsChanged)),
        ResourceCapabilities(capabilities.contains(ResourcesChanged)),
        if (capabilities.contains(Experimental)) Unit else null,
        if (capabilities.contains(Completions)) Unit else null,
        if (capabilities.contains(Logging)) Unit else null,
    )

    @JsonSerializable
    data class ToolCapabilities(val listChanged: Boolean? = false)

    @JsonSerializable
    data class PromptCapabilities(val listChanged: Boolean = false)

    @JsonSerializable
    data class ResourceCapabilities(val subscribe: Boolean? = false, val listChanged: Boolean? = false)
}

