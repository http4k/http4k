package org.http4k.mcp.server

/**
 * Determines what features the client supports.
 */
data class ClientCapabilities(
    val roots: RootCapabilities? = null,
    val experimental: Unit? = null,
    val sampling: Unit? = null,
) {
    companion object {
        data class RootCapabilities(val listChanged: Boolean? = false)
    }
}
