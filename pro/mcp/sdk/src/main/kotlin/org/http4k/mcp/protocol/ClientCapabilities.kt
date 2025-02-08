package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.ProtocolCapability.Experimental
import org.http4k.mcp.protocol.ProtocolCapability.RootChanged
import org.http4k.mcp.protocol.ProtocolCapability.Sampling
import se.ansman.kotshi.JsonSerializable

/**
 * Determines what features the client supports.
 */
@JsonSerializable
@ConsistentCopyVisibility
data class ClientCapabilities internal constructor(
    val roots: Root? = null,
    val experimental: Unit? = null,
    val sampling: Unit? = null,
) {
    constructor(vararg capabilities: ProtocolCapability) : this(
        Root(capabilities.contains(RootChanged)),
        if (capabilities.contains(Experimental)) Unit else null,
        if (capabilities.contains(Sampling)) Unit else null,
    )

    companion object {
        @JsonSerializable
        data class Root(val listChanged: Boolean? = false)
    }
}
