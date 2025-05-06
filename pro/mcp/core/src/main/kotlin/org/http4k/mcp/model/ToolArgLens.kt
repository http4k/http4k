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

class ToolArgLens<OUT>(
    meta: Meta,
    get: (ToolRequest) -> OUT,
    private val set: (OUT, ToolRequest) -> ToolRequest,
    private val toSchema: (ToolArgLens<*>) -> McpNodeType
) : LensInjectorExtractor<ToolRequest, OUT>, BiDiLens<ToolRequest, OUT>(meta, get, set) {

    @Suppress("UNCHECKED_CAST")
    override fun <R : ToolRequest> invoke(value: OUT, target: R): R = set(value, target) as R

    fun toSchema() = toSchema(this)
}
