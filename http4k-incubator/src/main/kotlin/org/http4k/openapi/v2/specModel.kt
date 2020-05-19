package org.http4k.openapi.v2

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.http4k.openapi.InfoSpec
import org.http4k.openapi.SchemaSpec

data class PathV2Spec(
    val operationId: String?,
    val consumes: List<String> = emptyList(),
    val parameters: List<ParameterSpec> = emptyList()
)


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "in", defaultImpl = SchemaSpec.RefSpec::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = ParameterSpec.CookieSpec::class, name = "cookie"),
    JsonSubTypes.Type(value = ParameterSpec.HeaderSpec::class, name = "header"),
    JsonSubTypes.Type(value = ParameterSpec.PathSpec::class, name = "path"),
    JsonSubTypes.Type(value = ParameterSpec.QuerySpec::class, name = "query"),
    JsonSubTypes.Type(value = ParameterSpec.FormSpec::class, name = "formData"),
    JsonSubTypes.Type(value = ParameterSpec.BodySpec::class, name = "body")
)
sealed class ParameterSpec(val name: String, val required: Boolean) {
    class CookieSpec(name: String, required: Boolean, val type: String) : ParameterSpec(name, required)
    class HeaderSpec(name: String, required: Boolean, val type: String) : ParameterSpec(name, required)
    class PathSpec(name: String, required: Boolean, val type: String) : ParameterSpec(name, required)
    class QuerySpec(name: String, required: Boolean, val type: String) : ParameterSpec(name, required)
    class FormSpec(name: String, required: Boolean, val type: String) : ParameterSpec(name, required)
    class BodySpec(name: String, required: Boolean, val schema: SchemaSpec) : ParameterSpec(name, required)
}

data class OpenApi2Spec(val info: InfoSpec, val paths: Map<String, Map<String, PathV2Spec>>, private val definitions: Map<String, SchemaSpec>?) {
    val components = definitions ?: emptyMap()
}
