package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.ClientProtocolCapability.Experimental
import org.http4k.mcp.protocol.ClientProtocolCapability.RootChanged
import org.http4k.mcp.protocol.ClientProtocolCapability.Sampling
import se.ansman.kotshi.JsonSerializable

/**
 * Determines what features the client supports.
 */
@JsonSerializable
@ConsistentCopyVisibility
data class ClientCapabilities internal constructor(
    val roots: Roots? = null,
    val sampling: Unit? = null,
    val experimental: Unit? = null,
) {
    constructor(vararg capabilities: ClientProtocolCapability = ClientProtocolCapability.entries.toTypedArray()) : this(
        Roots(capabilities.contains(RootChanged)),
        if (capabilities.contains(Sampling)) Unit else null,
        if (capabilities.contains(Experimental)) Unit else null,
    )

    companion object {
        val All = ClientCapabilities(Roots(true), Unit, Unit)

        @JsonSerializable
        data class Roots(val listChanged: Boolean? = false)
    }
}
