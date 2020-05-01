package org.http4k.openapi

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import java.math.BigDecimal

@JsonTypeInfo(use = NAME, property = "type", defaultImpl = SchemaSpec.RefSpec::class)
@JsonSubTypes(
    Type(value = SchemaSpec.ObjectSpec::class, name = "object"),
    Type(value = SchemaSpec.ArraySpec::class, name = "array"),
    Type(value = SchemaSpec.ArraySpec::class, name = "array"),
    Type(value = SchemaSpec.StringSpec::class, name = "string"),
    Type(value = SchemaSpec.NumberSpec::class, name = "number"),
    Type(value = SchemaSpec.IntegerSpec::class, name = "integer"),
    Type(value = SchemaSpec.BooleanSpec::class, name = "boolean")
)
sealed class SchemaSpec(val clazz: String? = null) {
    data class ObjectSpec(val required: List<String> = emptyList(), val properties: Map<String, SchemaSpec> = emptyMap()) : SchemaSpec()
    data class ArraySpec(val required: List<String> = emptyList(), val properties: Map<String, SchemaSpec> = emptyMap()) : SchemaSpec()
    object IntegerSpec : SchemaSpec(Int::class.qualifiedName)
    object NumberSpec : SchemaSpec(BigDecimal::class.qualifiedName)
    object StringSpec : SchemaSpec(String::class.qualifiedName)
    object BooleanSpec : SchemaSpec(Boolean::class.qualifiedName)
    data class RefSpec(val `$ref`: String) : SchemaSpec(String::class.qualifiedName)
}

@JsonTypeInfo(use = NAME, property = "in")
@JsonSubTypes(
    Type(value = ParameterSpec.PathSpec::class, name = "path"),
    Type(value = ParameterSpec.HeaderSpec::class, name = "header"),
    Type(value = ParameterSpec.QuerySpec::class, name = "query"),
    Type(value = ParameterSpec.CookieSpec::class, name = "cookie")
)
sealed class ParameterSpec(val name: String, val required: Boolean, val description: String?, val schema: SchemaSpec) {
    class PathSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class HeaderSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class QuerySpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class CookieSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
}

data class ResponseSpec(val schema: SchemaSpec?)
data class ComponentsSpec(val schemas: Map<String, SchemaSpec> = emptyMap())
data class PathSpec(
    val operationId: String?,
    val summary: String?,
    val description: String?,
    val tags: List<String> = emptyList(),
    val responses: Map<Int, ResponseSpec> = emptyMap(),
    val parameters: List<ParameterSpec> = emptyList()
)

data class InfoSpec(val title: String)
data class OpenApi3Spec(val info: InfoSpec, val paths: Map<String, Map<String, PathSpec>>, val components: ComponentsSpec)
