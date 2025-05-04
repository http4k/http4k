package org.http4k.mcp.model

import org.http4k.lens.BiDiLens
import org.http4k.lens.LensInjectorExtractor
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType

open class ToolArgLens<OUT>(
    meta: Meta,
    get: (ToolRequest) -> OUT,
    private val set: (OUT, ToolRequest) -> ToolRequest,
    private val toSchema: (Meta) -> McpNodeType
) : LensInjectorExtractor<ToolRequest, OUT>, BiDiLens<ToolRequest, OUT>(meta, get, set) {

    @Suppress("UNCHECKED_CAST")
    override fun <R : ToolRequest> invoke(value: OUT, target: R): R = set(value, target) as R

    open fun toSchema() = toSchema(meta)
}

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
