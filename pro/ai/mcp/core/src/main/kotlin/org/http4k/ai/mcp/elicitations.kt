package org.http4k.ai.mcp

import org.http4k.ai.mcp.model.ElicitationAction
import org.http4k.ai.mcp.model.ElicitationId
import org.http4k.ai.mcp.model.McpCapabilityLens
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Meta.Companion.default
import org.http4k.ai.mcp.model.ProgressToken
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpJson.obj
import org.http4k.ai.mcp.util.McpJson.string
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.Uri
import org.http4k.lens.McpLensTarget
import org.http4k.lens.ParamMeta.ObjectParam

/**
 *  Processes a elicitation request from an MCP server to a client
 */
typealias ElicitationHandler = (ElicitationRequest) -> ElicitationResponse

fun interface ElicitationFilter {
    operator fun invoke(handler: ElicitationHandler): ElicitationHandler

    companion object
}

val ElicitationFilter.Companion.NoOp: ElicitationFilter get() = ElicitationFilter { it }

fun ElicitationFilter.then(next: ElicitationFilter): ElicitationFilter = ElicitationFilter { this(next(it)) }

fun ElicitationFilter.then(next: ElicitationHandler): ElicitationHandler = this(next)

sealed class ElicitationRequest : McpLensTarget {
    abstract val message: String
    abstract val progressToken: ProgressToken?

    data class Form(
        override val message: String,
        val requestedSchema: McpNodeType = obj(),
        override val progressToken: ProgressToken? = null
    ) : ElicitationRequest() {
        constructor(
            message: String,
            vararg outputs: McpCapabilityLens<ElicitationResponse, *>,
            progressToken: ProgressToken? = null
        ) : this(
            message,
            when {
                outputs.first().meta.paramMeta == ObjectParam -> when (outputs.size) {
                    1 -> outputs.first().toSchema()
                    else -> error("only one Object allowed in outputs")
                }

                else -> toSchema(outputs.toList())
            },
            progressToken
        )
    }

    data class Url(
        override val message: String,
        val url: Uri,
        val elicitationId: ElicitationId,
        override val progressToken: ProgressToken? = null
    ) : ElicitationRequest()
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
