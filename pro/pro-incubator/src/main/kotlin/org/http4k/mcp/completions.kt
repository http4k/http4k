package org.http4k.mcp

import org.http4k.core.Request
import org.http4k.mcp.model.Completion
import org.http4k.mcp.model.CompletionArgument
import org.http4k.mcp.model.Reference

typealias CompletionHandler = (CompletionRequest) -> CompletionResponse

data class CompletionRequest(
    val ref: Reference,
    val argument: CompletionArgument,
    val connectRequest: Request
)

data class CompletionResponse(val completion: Completion)
