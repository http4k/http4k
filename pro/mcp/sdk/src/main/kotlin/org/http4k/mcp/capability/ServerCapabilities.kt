package org.http4k.mcp.capability

/**
 * Provides the facility to compose many capabilities together.
 */
class ServerCapabilities(private vararg val bindings: ServerCapability) : ServerCapability {
    override fun iterator() = bindings.iterator()
}
