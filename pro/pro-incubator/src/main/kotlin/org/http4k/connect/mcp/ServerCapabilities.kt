package org.http4k.connect.mcp

data class ServerCapabilities(
    val tools: Tools = Tools(),
    val prompts: Prompts = Prompts(),
    val resources: Resources = Resources(),
    val experimental: Unit? = null,
    val logging: Unit? = null,
    val sampling: Unit? = null,
) {
    data class Tools(val listChanged: Boolean? = true)
    data class Prompts(val listChanged: Boolean = true)
    data class Resources(val subscribe: Boolean? = true, val listChanged: Boolean? = true)
}
