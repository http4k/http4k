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
