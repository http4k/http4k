package org.http4k.mcp.model

import org.http4k.lens.BiDiLens
import org.http4k.lens.LensInjectorExtractor
import org.http4k.lens.Meta
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.util.McpNodeType

class ToolLens<IN: Any, OUT>(
    meta: Meta,
    get: (IN) -> OUT,
    private val set: (OUT, IN) -> IN,
    private val toSchema: (ToolLens<IN, *>) -> McpNodeType
) : LensInjectorExtractor<IN, OUT>, BiDiLens<IN, OUT>(meta, get, set) {

    @Suppress("UNCHECKED_CAST")
    override fun <R : IN> invoke(value: OUT, target: R): R = set(value, target) as R

    fun toSchema() = toSchema(this)
}
