/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol

interface McpExtension {
    val name: String
    val config: Any
}

fun ServerMetaData.withExtensions(vararg extensions: McpExtension): ServerMetaData = ServerMetaData(
    entity.name,
    entity.version,
    capabilities.withExtensions(*extensions),
    entity.title,
    instructions,
    protocolVersions
)

fun ServerCapabilities.withExtensions(vararg extensions: McpExtension) = extensions.fold(this) { acc, ext ->
    acc.withExtensions(ext.name to ext.config)
}
