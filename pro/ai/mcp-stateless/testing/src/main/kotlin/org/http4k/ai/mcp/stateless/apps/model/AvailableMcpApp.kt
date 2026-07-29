/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.apps.model

import org.http4k.ai.mcp.stateless.model.McpUri
import org.http4k.ai.model.ToolName

data class AvailableMcpApp(
    val serverId: String,
    val serverName: String,
    val uiToolName: ToolName,
    val resourceUri: McpUri
)
