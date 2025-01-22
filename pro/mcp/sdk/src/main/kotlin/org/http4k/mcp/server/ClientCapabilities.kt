package org.http4k.mcp.server

/**
 * Determines what features the client supports.
 */
data class ClientCapabilities(
    val roots: Root? = null,
    val experimental: Unit? = null,
    val sampling: Unit? = null,
) {
    companion object {
        data class Root(val listChanged: Boolean? = false)
    }
}
