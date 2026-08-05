/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.prompts

import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.protocol.Prompts

/**
 * CapabilityPack containing Prompt tests defined in the the MCP Conformance Test Suite
 */
fun CondormancePrompts(): Prompts {
    val mutablePrompts = mutableListOf(
        simplePrompt(),
        argumentsPrompt(),
        imagePrompt(),
        embeddedResourcePrompt(),
        inputRequiredResultPrompt(),
    )
    return prompts(mutablePrompts)
}
