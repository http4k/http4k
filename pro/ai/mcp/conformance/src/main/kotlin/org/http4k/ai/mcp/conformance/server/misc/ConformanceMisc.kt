package org.http4k.ai.mcp.conformance.server.misc

import org.http4k.ai.mcp.conformance.server.prompts.emptyCompletion
import org.http4k.ai.mcp.server.capability.CapabilityPack

/**
 * CapabilityPack containing miscellaneous tests defined in the the MCP Conformance Test Suite
 */
fun ConformanceMisc() = CapabilityPack(
    emptyCompletion()
)
