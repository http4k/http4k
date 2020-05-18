package org.http4k.openapi.v3

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.http4k.openapi.InfoSpec
import org.http4k.openapi.SchemaSpec

data class RequestBodyV3Spec(val content: Map<String, MessageBodyV3Spec> = emptyMap())

data class MessageBodyV3Spec(val schema: SchemaSpec?)

data class ComponentsV3Spec(val schemas: Map<String, SchemaSpec> = emptyMap())

data class ResponseV3Spec(val content: Map<String, MessageBodyV3Spec>)

data class PathV3Spec(
    val operationId: String?,
    val summary: String?,
    val description: String?,
    val tags: List<String> = emptyList(),
    val responses: Map<Int, ResponseV3Spec> = emptyMap(),
    val requestBody: RequestBodyV3Spec?,
    val parameters: List<ParameterSpec> = emptyList()
)

data class OpenApi3Spec(val info: InfoSpec, val paths: Map<String, Map<String, PathV3Spec>>, val components: ComponentsV3Spec = ComponentsV3Spec())

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "in")
@JsonSubTypes(
    JsonSubTypes.Type(value = ParameterSpec.PathSpec::class, name = "path"),
    JsonSubTypes.Type(value = ParameterSpec.HeaderSpec::class, name = "header"),
    JsonSubTypes.Type(value = ParameterSpec.QuerySpec::class, name = "query"),
    JsonSubTypes.Type(value = ParameterSpec.CookieSpec::class, name = "cookie")
)
sealed class ParameterSpec(val name: String, val required: Boolean, val description: String?, val schema: SchemaSpec) {
    class CookieSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class HeaderSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class PathSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class QuerySpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
}
