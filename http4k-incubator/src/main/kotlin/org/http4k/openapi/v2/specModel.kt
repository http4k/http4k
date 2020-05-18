package org.http4k.openapi.v2

import org.http4k.openapi.InfoSpec
import org.http4k.openapi.SchemaSpec

data class PathV2Spec(
    val operationId: String?,
    val parameters: List<ParameterSpec> = emptyList()
)

data class ParameterSpec(val `in`: String, val name: String, val required: Boolean, val type: String)

data class OpenApi2Spec(val info: InfoSpec, val paths: Map<String, Map<String, PathV2Spec>>, private val definitions: Map<String, SchemaSpec>?) {
    val components = definitions ?: emptyMap()
}
