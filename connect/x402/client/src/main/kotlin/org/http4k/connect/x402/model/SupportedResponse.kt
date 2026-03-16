package org.http4k.connect.x402.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SupportedResponse(
    val x402Version: Int,
    val kinds: List<SupportedKind>
)
