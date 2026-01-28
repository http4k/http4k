package org.http4k.ai.mcp

import org.http4k.ai.mcp.Client.Companion.NoOp
import org.http4k.ai.mcp.model.CompletionArgument
import org.http4k.ai.mcp.model.CompletionContext
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Meta.Companion.default
import org.http4k.core.Request

/**
 * A CompletionHandler is a function which creates a Completion from a set of inputs
 */
typealias CompletionHandler = (CompletionRequest) -> CompletionResponse

fun interface CompletionFilter {
    operator fun invoke(handler: CompletionHandler): CompletionHandler

    companion object
}

val CompletionFilter.Companion.NoOp: CompletionFilter get() = CompletionFilter { it }

fun CompletionFilter.then(next: CompletionFilter): CompletionFilter = CompletionFilter { this(next(it)) }

fun CompletionFilter.then(next: CompletionHandler): CompletionHandler = this(next)

data class CompletionRequest(
    val argument: CompletionArgument,
    val context: CompletionContext = CompletionContext(),
    override val meta: Meta = default,
    val client: Client = NoOp,
    val connectRequest: Request? = null
) : CapabilityRequest {
    constructor(
        name: String,
        value: String,
        context: CompletionContext = CompletionContext(),
        meta: Meta = default,
        client: Client = NoOp,
        connectRequest: Request? = null
    ) : this(CompletionArgument(name, value), context, meta, client, connectRequest)
}


data class CompletionResponse(val values: List<String>, val total: Int? = null, val hasMore: Boolean? = null)
