package org.http4k.mcp.capability

/**
 * Provides the facility to compose many capability bindings together.
 */
class CapabilityBindings(private vararg val bindings: CapabilityBinding) : CapabilityBinding {
    override fun iterator() = bindings.iterator()
}
