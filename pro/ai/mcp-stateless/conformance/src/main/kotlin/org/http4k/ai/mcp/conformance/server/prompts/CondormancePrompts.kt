/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.server.capability.capabilities

/**
 * CapabilityPack containing Prompt tests defined in the the MCP Conformance Test Suite
 */
fun CondormancePrompts() = capabilities(
    simplePrompt(),
    argumentsPrompt(),
    imagePrompt(),
    embeddedResourcePrompt(),
    dynamicPrompt(),
)
