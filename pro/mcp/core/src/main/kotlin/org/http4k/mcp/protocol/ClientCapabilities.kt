package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.ClientProtocolCapability.Elicitation
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
    val roots: Roots?,
    val sampling: Unit?,
    val experimental: Unit?,
    val elicitation: Unit?,
) {
    constructor(vararg capabilities: ClientProtocolCapability = ClientProtocolCapability.entries.toTypedArray()) : this(
        Roots(capabilities.contains(RootChanged)),
        if (capabilities.contains(Sampling)) Unit else null,
        if (capabilities.contains(Experimental)) Unit else null,
        if (capabilities.contains(Elicitation)) Unit else null,
    )

    companion object {
        val All = ClientCapabilities(Roots(true), Unit, Unit, Unit)

        @JsonSerializable
        data class Roots(val listChanged: Boolean? = false)
    }
}
