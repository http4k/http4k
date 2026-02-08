package org.http4k.ai.mcp.model.apps

import org.http4k.ai.mcp.model.Domain
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpAppResourceMeta(
    val csp: Csp? = null,
    val permissions: Permissions? = null,
    val domain: Domain? = null,
    val prefersBorder: Boolean? = null
)
