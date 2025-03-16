package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Reference

/**
 * A CompletionHandler is a function which creates a Completion from a set of inputs
 */
typealias CompletionHandler = (CompletionRequest) -> CompletionResponse

data class CompletionRequest(
    val ref: Reference,
    val argument: CompletionArgument,
    val connectRequest: Request? = null
)

data class CompletionResponse(val values: List<String>, val total: Int? = null, val hasMore: Boolean? = null)
