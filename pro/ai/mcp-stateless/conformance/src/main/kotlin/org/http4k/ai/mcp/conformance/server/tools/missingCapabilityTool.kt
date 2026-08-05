/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.conformance.server.tools

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.MissingRequiredClientCapabilityError
import org.http4k.lens.MetaKey
import org.http4k.lens.clientCapabilities
import org.http4k.routing.bind

fun missingCapabilityTool() =
    Tool("test_missing_capability", "test_missing_capability") bind {
        if (MetaKey.clientCapabilities().toLens()(it.meta)?.sampling == null) {
            throw McpException(MissingRequiredClientCapabilityError(listOf("sampling")))
        }
        Ok(textContent)
    }
