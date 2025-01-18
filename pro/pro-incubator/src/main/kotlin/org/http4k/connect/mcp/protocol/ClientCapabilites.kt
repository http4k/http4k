package org.http4k.connect.mcp.protocol

data class ClientCapabilites(
    val roots: Roots? = null,
    val experimental: Unit? = null,
    val sampling: Unit? = null,
) {
    companion object {
        data class Roots(val listChanged: Boolean? = false)
    }
}
