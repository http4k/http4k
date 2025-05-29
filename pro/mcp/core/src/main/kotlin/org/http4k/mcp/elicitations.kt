package org.http4k.mcp

import org.http4k.mcp.model.ElicitationAction
import org.http4k.mcp.model.Meta
import org.http4k.mcp.model.ProgressToken
import org.http4k.mcp.util.McpNodeType

/**
 *  Processes a elicitation request from an MCP server to a client
 */
typealias ElicitationHandler = (ElicitationRequest) -> ElicitationResponse

fun interface ElicitationFilter {
    operator fun invoke(request: ElicitationHandler): ElicitationHandler

    companion object
}

val ElicitationFilter.Companion.NoOp: ElicitationFilter get() = ElicitationFilter { it }

fun ElicitationFilter.then(next: ElicitationFilter): ElicitationFilter = ElicitationFilter { this(next(it)) }

fun ElicitationFilter.then(next: ElicitationHandler): ElicitationHandler = this(next)

data class ElicitationRequest(val message: String, val requestedSchema: McpNodeType, val progressToken: ProgressToken? = null)

data class ElicitationResponse(val action: ElicitationAction, val content: McpNodeType, val _meta: Meta = Meta.default)
