/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Represents the name of the MCP entity. Used for identification and matching purposes.
 */
class McpEntity private constructor(value: String) : StringValue(value), CapabilitySpec {
    companion object : NonBlankStringValueFactory<McpEntity>(::McpEntity)
}
