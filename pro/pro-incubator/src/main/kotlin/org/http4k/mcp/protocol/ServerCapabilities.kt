package org.http4k.mcp.protocol

data class ServerCapabilities(
    val tools: Tools = Tools(),
    val prompts: Prompts = Prompts(),
    val resources: Resources = Resources(),
    val experimental: Unit = Unit,
    val logging: Unit = Unit,
    val sampling: Unit = Unit,
) {
    data class Tools(val listChanged: Boolean? = true)
    data class Prompts(val listChanged: Boolean = true)
    data class Resources(val subscribe: Boolean? = true, val listChanged: Boolean? = true)
}
