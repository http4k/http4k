/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.InitializeHandler
import org.http4k.ai.mcp.InitializeRequest
import org.http4k.ai.mcp.InitializeResponse
import org.http4k.ai.mcp.protocol.ServerMetaData

/**
 * Default implementation of InitializeHandler that uses the ServerMetaData to negotiate the protocol version
 */
fun SimpleInitializeHandler(metaData: ServerMetaData): InitializeHandler = { it: InitializeRequest ->
    InitializeResponse.Ok(
        metaData.entity, metaData.capabilities, when {
            metaData.protocolVersions.contains(it.protocolVersion) -> it.protocolVersion
            else -> metaData.protocolVersions.max()
        },
        metaData.instructions
    )
}
