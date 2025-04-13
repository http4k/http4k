package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Meta
import org.http4k.mcp.Client.Companion.NoOp

/**
 * A CompletionHandler is a function which creates a Completion from a set of inputs
 */
typealias CompletionHandler = (CompletionRequest) -> CompletionResponse

fun interface CompletionFilter {
    operator fun invoke(request: CompletionHandler): CompletionHandler
    companion object
}

val CompletionFilter.Companion.NoOp: CompletionFilter get() = CompletionFilter { it }

fun CompletionFilter.then(next: CompletionFilter): CompletionFilter = CompletionFilter { this(next(it)) }

fun CompletionFilter.then(next: CompletionHandler): CompletionHandler = this(next)

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
