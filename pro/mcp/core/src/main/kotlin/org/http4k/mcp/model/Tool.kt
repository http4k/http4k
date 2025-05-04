package org.http4k.mcp.model

import org.http4k.connect.model.ToolName
import org.http4k.lens.BiDiLens
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.Lens
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType

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

inline fun <reified T> Lens<ToolRequest, T>.toSchema(): McpNodeType =
    meta.paramMeta.toSchema(meta.description)

fun ParamMeta.toSchema(description: String? = null): McpNodeType =
    McpJson.asJsonObject(
        when (this) {
            is ArrayParam, ObjectParam -> mapOf(
                "type" to this.description,
                "items" to (this as ArrayParam).itemType().toSchema(),
                "description" to description,
            )

            is EnumParam<*> -> mapOf(
                "type" to this.description,
                "enum" to clz.java.enumConstants?.map { it.name },
                "description" to description,
            )

            else -> mapOf(
                "type" to this.description,
                "description" to description,
            )
        }
    )


