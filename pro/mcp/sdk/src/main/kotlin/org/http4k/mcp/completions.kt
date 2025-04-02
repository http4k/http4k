package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Meta
import org.http4k.mcp.server.protocol.Client
import org.http4k.mcp.server.protocol.Client.Companion.NoOp

/**
 * A CompletionHandler is a function which creates a Completion from a set of inputs
 */
typealias CompletionHandler = (CompletionRequest) -> CompletionResponse

data class CompletionRequest(
    val argument: CompletionArgument,
    val meta: Meta = Meta.default,
    val client: Client = NoOp,
    val connectRequest: Request? = null
) {
    constructor(
        name: String,
        value: String,
        meta: Meta = Meta.default,
        client: Client = NoOp,
        connectRequest: Request? = null
    ) : this(CompletionArgument(name, value), meta, client, connectRequest)
}

data class CompletionResponse(val values: List<String>, val total: Int? = null, val hasMore: Boolean? = null)
