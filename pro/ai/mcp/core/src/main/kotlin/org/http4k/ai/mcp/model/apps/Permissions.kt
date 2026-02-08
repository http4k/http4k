package org.http4k.ai.mcp.model.apps

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Permissions(
    val camera: Unit? = null,
    val microphone: Unit? = null,
    val geolocation: Unit? = null,
    val clipboardWrite: Unit? = null
)
