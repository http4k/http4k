package org.http4k.ai.mcp.conformance.server.resources

import org.http4k.ai.mcp.server.capability.CapabilityPack

/**
 * CapabilityPack containing Resource tests defined in the the MCP Conformance Test Suite
 */
fun ConformanceResources() = CapabilityPack(
    staticTextResource(),
    staticBinaryResource(),
    templateResource(),
    watchedResource(),
    dynamicResource()
)
