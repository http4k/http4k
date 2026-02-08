package org.http4k.ai.mcp.model.apps

import org.http4k.ai.mcp.model.Domain
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpAppResourceMeta(
    val csp: McpAppCsp? = null,
    val permissions: McpAppPermissions? = null,
    val domain: Domain? = null,
    val prefersBorder: Boolean? = null
)
