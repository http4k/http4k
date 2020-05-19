package org.http4k.openapi.v3

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.http4k.openapi.InfoSpec
import org.http4k.openapi.MessageBodySpec
import org.http4k.openapi.ResponseSpec
import org.http4k.openapi.SchemaSpec

data class RequestBodyV3Spec(val content: Map<String, MessageBodySpec> = emptyMap())

data class ComponentsV3Spec(val schemas: Map<String, SchemaSpec> = emptyMap())

data class PathV3Spec(
    val operationId: String?,
    val responses: Map<Int, ResponseSpec> = emptyMap(),
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
sealed class ParameterSpec(val name: String, val required: Boolean, val schema: SchemaSpec) {
    class CookieSpec(name: String, required: Boolean, schema: SchemaSpec) : ParameterSpec(name, required, schema)
    class HeaderSpec(name: String, required: Boolean, schema: SchemaSpec) : ParameterSpec(name, required, schema)
    class PathSpec(name: String, required: Boolean, schema: SchemaSpec) : ParameterSpec(name, required, schema)
    class QuerySpec(name: String, required: Boolean, schema: SchemaSpec) : ParameterSpec(name, required, schema)
}
