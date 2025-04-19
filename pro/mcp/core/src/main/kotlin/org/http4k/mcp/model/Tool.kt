package org.http4k.mcp.model

import org.http4k.connect.model.ToolName
import org.http4k.lens.BiDiLens
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.protocol.messages.McpTool

/**
 * Description of a Tool capability.
 */
class Tool private constructor(
    val name: ToolName,
    val description: String,
    val args: List<BiDiLens<ToolRequest, *>>,
    val annotations: ToolAnnotations? = null,
) : CapabilitySpec {
    constructor(
        name: String,
        description: String,
        vararg arguments: BiDiLens<ToolRequest, *>,
        annotations: ToolAnnotations? = null
    ) : this(ToolName.of(name), description, arguments.toList(), annotations)

    object Arg : BiDiLensSpec<ToolRequest, String>(
        "toolRequest", StringParam,
        LensGet { name, target ->
            when (val value = target[name]) {
                null -> emptyList()
                is List<*> -> value.map { it.toString() }
                else -> listOf(value.toString())
            }
        },
        LensSet { name, values, target -> values.fold(target) { m, v -> m.copy(args = m + (name to v)) } }
    )
}

fun Tool.toSchema() = mapOf(
    "type" to "object",
    "required" to args.filter { it.meta.required }.map { it.meta.name },
    "properties" to mapOf(
        *args.map {
            it.meta.name to mapOf(
                "type" to it.meta.paramMeta.description,
                "description" to it.meta.description,
            )
        }.toTypedArray()
    )
)

