package org.http4k.mcp.server.capability

/**
 * Provides the facility to compose many capabilities together.
 */
class CapabilityPack(private vararg val bindings: ServerCapability) : ServerCapability {
    override fun iterator() = bindings.iterator()
}
