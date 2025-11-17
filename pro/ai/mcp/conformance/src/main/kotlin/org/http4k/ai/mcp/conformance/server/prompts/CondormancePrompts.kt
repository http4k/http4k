package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.server.capability.CapabilityPack

/**
 * CapabilityPack containing Prompt tests defined in the the MCP Conformance Test Suite
 */
fun CondormancePrompts() = CapabilityPack(
    simplePrompt(),
    argumentsPrompt(),
    imagePrompt(),
    embeddedResourcePrompt(),
    dynamicPrompt(),
)
