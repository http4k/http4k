package org.http4k.mcp.protocol

import se.ansman.kotshi.JsonSerializable

/**
 * Determines what features the client supports.
 */
@JsonSerializable
data class ClientCapabilities(
    val roots: Root? = null,
    val experimental: Unit? = null,
    val sampling: Unit? = null,
) {
    companion object {
        @JsonSerializable
        data class Root(val listChanged: Boolean? = false)
    }
}
