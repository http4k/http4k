package org.http4k.mcp.server

data class ServerCapabilities(
    val tools: ToolCapabilities = ToolCapabilities(),
    val prompts: PromptCapabilities = PromptCapabilities(),
    val resources: ResourceCapabilities = ResourceCapabilities(),
    val experimental: Unit? = null,
    val logging: Unit? = null,
    val sampling: Unit? = null,
) {
    data class ToolCapabilities(val listChanged: Boolean? = true)
    data class PromptCapabilities(val listChanged: Boolean = true)
    data class ResourceCapabilities(val subscribe: Boolean? = true, val listChanged: Boolean? = true)
}
