package org.http4k.openapi.v3

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.http4k.openapi.InfoSpec
import org.http4k.openapi.MessageBodySpec
import org.http4k.openapi.ResponseSpec
import org.http4k.openapi.SchemaSpec
import org.http4k.openapi.cleanSchemaName
import org.http4k.openapi.cleanValueName

data class OpenApi3RequestBodySpec(
    val description: String? = null,
    val content: Map<String, MessageBodySpec> = emptyMap()
)

data class OpenApi3ComponentsSpec(val schemas: Map<String, SchemaSpec> = emptyMap(), val parameters: Map<String, OpenApi3ParameterSpec> = emptyMap())

data class OpenApi3PathSpec(
    val summary: String?,
    val description: String?,
    val operationId: String?,
    val responses: Map<Int, ResponseSpec> = emptyMap(),
    val requestBody: OpenApi3RequestBodySpec = OpenApi3RequestBodySpec(),
    val parameters: List<OpenApi3ParameterSpec> = emptyList()
)

data class OpenApi3Spec(val info: InfoSpec, val paths: Map<String, Map<String, OpenApi3PathSpec>>, val components: OpenApi3ComponentsSpec = OpenApi3ComponentsSpec())

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "in", defaultImpl = OpenApi3ParameterSpec.RefSpec::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = OpenApi3ParameterSpec.PathSpec::class, name = "path"),
    JsonSubTypes.Type(value = OpenApi3ParameterSpec.HeaderSpec::class, name = "header"),
    JsonSubTypes.Type(value = OpenApi3ParameterSpec.QuerySpec::class, name = "query"),
    JsonSubTypes.Type(value = OpenApi3ParameterSpec.CookieSpec::class, name = "cookie"),
    JsonSubTypes.Type(value = OpenApi3ParameterSpec.FormFieldSpec::class, name = "formData")
)
sealed class OpenApi3ParameterSpec(name: String, val required: Boolean, val schema: SchemaSpec) {
    val name = name.cleanValueName()

    class CookieSpec(name: String, required: Boolean, schema: SchemaSpec) : OpenApi3ParameterSpec(name, required, schema)
    class HeaderSpec(name: String, required: Boolean, schema: SchemaSpec) : OpenApi3ParameterSpec(name, required, schema)
    class PathSpec(name: String, required: Boolean, schema: SchemaSpec) : OpenApi3ParameterSpec(name, required, schema)
    class QuerySpec(name: String, required: Boolean, schema: SchemaSpec) : OpenApi3ParameterSpec(name, required, schema)
    class FormFieldSpec(name: String, required: Boolean, schema: SchemaSpec) : OpenApi3ParameterSpec(name, required, schema)
    class RefSpec(val `$ref`: String) : OpenApi3ParameterSpec(`$ref`, false, SchemaSpec.RefSpec(`$ref`)) {
        val schemaName = `$ref`.cleanSchemaName().cleanValueName()
    }
}
