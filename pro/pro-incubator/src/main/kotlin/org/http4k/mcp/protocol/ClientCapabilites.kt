package org.http4k.mcp.protocol

data class ClientCapabilites(
    val roots: org.http4k.mcp.protocol.ClientCapabilites.Companion.Roots? = null,
    val experimental: Unit? = null,
    val sampling: Unit? = null,
) {
    companion object {
        data class Roots(val listChanged: Boolean? = false)
    }
}
