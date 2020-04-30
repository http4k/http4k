package org.http4k.openapi

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = SchemaSpec.RefSpec::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = SchemaSpec.ObjectSpec::class, name = "object"),
    JsonSubTypes.Type(value = SchemaSpec.ArraySpec::class, name = "array"),
    JsonSubTypes.Type(value = SchemaSpec.ArraySpec::class, name = "array"),
    JsonSubTypes.Type(value = SchemaSpec.StringSpec::class, name = "string"),
    JsonSubTypes.Type(value = SchemaSpec.NumberSpec::class, name = "number"),
    JsonSubTypes.Type(value = SchemaSpec.IntegerSpec::class, name = "integer"),
    JsonSubTypes.Type(value = SchemaSpec.BooleanSpec::class, name = "boolean")
)
sealed class SchemaSpec {
    data class ObjectSpec(val required: List<String> = emptyList(), val properties: Map<String, SchemaSpec> = emptyMap()) : SchemaSpec()
    data class ArraySpec(val required: List<String> = emptyList(), val properties: Map<String, SchemaSpec> = emptyMap()) : SchemaSpec()
    object IntegerSpec : SchemaSpec()
    object NumberSpec : SchemaSpec()
    object StringSpec : SchemaSpec()
    object BooleanSpec : SchemaSpec()
    data class RefSpec(val `$ref`: String) : SchemaSpec()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "in")
@JsonSubTypes(
    JsonSubTypes.Type(value = ParameterSpec.PathSpec::class, name = "path"),
    JsonSubTypes.Type(value = ParameterSpec.HeaderSpec::class, name = "header"),
    JsonSubTypes.Type(value = ParameterSpec.QuerySpec::class, name = "query"),
    JsonSubTypes.Type(value = ParameterSpec.CookieSpec::class, name = "cookie")
)
sealed class ParameterSpec(val name: String, val required: Boolean, val description: String?) {
    class PathSpec(name: String, required: Boolean, description: String?) : ParameterSpec(name, required, description)
    class HeaderSpec(name: String, required: Boolean, description: String?) : ParameterSpec(name, required, description)
    class QuerySpec(name: String, required: Boolean, description: String?) : ParameterSpec(name, required, description)
    class CookieSpec(name: String, required: Boolean, description: String?) : ParameterSpec(name, required, description)
}

data class ResponseSpec(val schema: SchemaSpec?)
data class ComponentsSpec(val schemas: Map<String, SchemaSpec> = emptyMap())
data class PathSpec(
    val operationId: String?,
    val summary: String?,
    val description: String?,
    val tags: List<String>? = emptyList(),
    val responses: Map<Int, ResponseSpec>? = emptyMap(),
    val parameters: List<ParameterSpec>? = emptyList()
)
data class OpenApi3Spec(val paths: Map<String, Map<String, PathSpec>>, val components: ComponentsSpec)
