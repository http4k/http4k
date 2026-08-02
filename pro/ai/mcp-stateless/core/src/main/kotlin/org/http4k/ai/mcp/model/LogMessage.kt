/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.util.McpJson.obj
import org.http4k.ai.mcp.util.McpNodeType

/** A `notifications/message` payload handed to a client `onLog` callback. */
data class LogMessage(val data: McpNodeType = obj(), val level: LogLevel, val logger: String? = null)
