package org.http4k.connect.mcp

data class ServerCapabilities(
    val tools: Tools? = null,
    val prompts: Prompts? = null,
    val resources: Resources? = null,
    val experimental: Unit? = null,
    val logging: Unit? = null,
    val sampling: Unit? = null,
) {
    companion object {
        data class Tools(val listChanged: Boolean? = false)
        data class Prompts(val listChanged: Boolean = false)
        data class Resources(val subscribe: Boolean? = false, val listChanged: Boolean? = false)
    }
}
