package org.http4k.ai.mcp.model

import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.EnumParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

fun Meta.toSchema() = paramMeta.toSchema(description, metadata)

fun ParamMeta.toSchema(description: String?, metadata: Map<String, Any>): McpNodeType = McpJson.asJsonObject(
    when (this) {
        is ArrayParam, ObjectParam -> mapOf(
            "type" to this.description,
            "items" to (this as ArrayParam).itemType().toSchema(null, emptyMap()),
            "description" to description,
        ) + metadata

        is EnumParam<*> -> mapOf(
            "type" to this.description,
            "enum" to clz.java.enumConstants?.map { it.name },
            "description" to description,
        ) + metadata

        else -> mapOf(
            "type" to this.description,
            "description" to description,
        ) + metadata
    }
)
