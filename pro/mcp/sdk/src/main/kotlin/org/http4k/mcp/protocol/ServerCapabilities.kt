package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.ServerProtocolCapability.Experimental
import org.http4k.mcp.protocol.ServerProtocolCapability.Logging
import org.http4k.mcp.protocol.ServerProtocolCapability.PromptsChanged
import org.http4k.mcp.protocol.ServerProtocolCapability.ResourcesChanged
import org.http4k.mcp.protocol.ServerProtocolCapability.Completions
import org.http4k.mcp.protocol.ServerProtocolCapability.ToolsChanged
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
@ConsistentCopyVisibility
data class ServerCapabilities internal constructor(
    val tools: ToolCapabilities,
    val prompts: PromptCapabilities,
    val resources: ResourceCapabilities,
    val completions: Unit?,
    val logging: Unit?,
    val experimental: Unit?
) {
    constructor(vararg capabilities: ServerProtocolCapability = ServerProtocolCapability.entries.toTypedArray()) : this(
        ToolCapabilities(capabilities.contains(ToolsChanged)),
        PromptCapabilities(capabilities.contains(PromptsChanged)),
        ResourceCapabilities(capabilities.contains(ResourcesChanged)),
        if (capabilities.contains(Completions)) Unit else null,
        if (capabilities.contains(Logging)) Unit else null,
        if (capabilities.contains(Experimental)) Unit else null,
    )

    @JsonSerializable
    data class ToolCapabilities(val listChanged: Boolean? = false)

    @JsonSerializable
    data class PromptCapabilities(val listChanged: Boolean = false)

    @JsonSerializable
    data class ResourceCapabilities(val subscribe: Boolean? = false, val listChanged: Boolean? = false)
}

