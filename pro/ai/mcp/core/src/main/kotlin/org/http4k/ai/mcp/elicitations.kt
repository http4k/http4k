package org.http4k.ai.mcp

import org.http4k.lens.McpLensTarget
import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.McpCapabilityLens
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Meta.Companion.default
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.obj
import org.http4k.ai.mcp.util.McpJson.string
import org.http4k.ai.mcp.util.McpNodeType

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

data class ElicitationRequest(
    val message: String,
    val requestedSchema: McpNodeType = obj(),
    val progressToken: ProgressToken? = null
) : McpLensTarget {
    constructor(
        message: String,
        vararg outputs: McpCapabilityLens<ElicitationResponse, *>,
        progressToken: ProgressToken? = null
    ) : this(message, toSchema(outputs.toList()), progressToken)
}

private fun toSchema(outputs: List<McpCapabilityLens<ElicitationResponse, *>>) = obj(
    "type" to string("object"),
    "properties" to obj(
        outputs.map { it.meta.name to it.toSchema() }.toList()
    ),
    "required" to McpJson.array(
        outputs.filter { it.meta.required }.map { string(it.meta.name) }
    )
)

data class ElicitationResponse(
    val action: ElicitationAction,
    val content: McpNodeType = obj(),
    val _meta: Meta = default
) : McpLensTarget
