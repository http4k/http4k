package org.http4k.ai.mcp.model.extension

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpAppPermissions(
    val camera: Unit? = null,
    val microphone: Unit? = null,
    val geolocation: Unit? = null,
    val clipboardWrite: Unit? = null
)
