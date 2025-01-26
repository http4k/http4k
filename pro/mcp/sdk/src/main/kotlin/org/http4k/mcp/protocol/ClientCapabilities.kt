package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.McpCapability.Experimental
import org.http4k.mcp.protocol.McpCapability.RootChanged
import org.http4k.mcp.protocol.McpCapability.Sampling
import se.ansman.kotshi.JsonSerializable

/**
 * Determines what features the client supports.
 */
@JsonSerializable
data class ClientCapabilities internal constructor(
    val roots: Root? = null,
    val experimental: Unit? = null,
    val sampling: Unit? = null,
) {
    constructor(vararg capabilities: McpCapability) : this(
        Root(capabilities.contains(RootChanged)),
        if (capabilities.contains(Experimental)) Unit else null,
        if (capabilities.contains(Sampling)) Unit else null,
    )

    companion object {
        @JsonSerializable
        data class Root(val listChanged: Boolean? = false)
    }
}
