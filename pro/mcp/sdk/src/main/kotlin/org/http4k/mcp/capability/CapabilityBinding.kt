package org.http4k.mcp.capability

/**
 * Marker interface for classes which are used to bind capabilities to the MCP server.
 */
sealed interface CapabilityBinding : Iterable<CapabilityBinding> {
    override fun iterator() = listOf(this).iterator()
}
