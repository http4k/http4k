package org.http4k.mcp.protocol

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ServerCapabilities(
    val tools: ToolCapabilities = ToolCapabilities(),
    val prompts: PromptCapabilities = PromptCapabilities(),
    val resources: ResourceCapabilities = ResourceCapabilities(),
    val experimental: Unit? = null,
    val logging: Unit? = null,
    val sampling: Unit? = null,
) {
    @JsonSerializable
    data class ToolCapabilities(val listChanged: Boolean? = true)

    @JsonSerializable
    data class PromptCapabilities(val listChanged: Boolean = true)

    @JsonSerializable
    data class ResourceCapabilities(val subscribe: Boolean? = true, val listChanged: Boolean? = true)
}
