package org.http4k.openapi.v2

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.http4k.openapi.InfoSpec
import org.http4k.openapi.MessageBodySpec
import org.http4k.openapi.SchemaSpec
import org.http4k.openapi.cleanSchemaName

data class OpenApi2PathSpec(
    val operationId: String?,
    val produces: List<String> = emptyList(),
    val consumes: List<String> = emptyList(),
    val responses: Map<String, MessageBodySpec> = emptyMap(),
    val parameters: List<OpenApi2ParameterSpec> = emptyList()
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "in", defaultImpl = OpenApi2ParameterSpec.RefSpec::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = OpenApi2ParameterSpec.CookieSpec::class, name = "cookie"),
    JsonSubTypes.Type(value = OpenApi2ParameterSpec.HeaderSpec::class, name = "header"),
    JsonSubTypes.Type(value = OpenApi2ParameterSpec.PathSpec::class, name = "path"),
    JsonSubTypes.Type(value = OpenApi2ParameterSpec.QuerySpec::class, name = "query"),
    JsonSubTypes.Type(value = OpenApi2ParameterSpec.FormSpec::class, name = "formData"),
    JsonSubTypes.Type(value = OpenApi2ParameterSpec.BodySpec::class, name = "body")
)
sealed class OpenApi2ParameterSpec(val name: String, val required: Boolean, val items: JsonNode = NullNode.instance) {
    fun itemsSpec(): SchemaSpec = SchemaSpec.ArraySpec(items).itemsSpec()

    class CookieSpec(name: String, required: Boolean, val type: String) : OpenApi2ParameterSpec(name, required)
    class HeaderSpec(name: String, required: Boolean, val type: String) : OpenApi2ParameterSpec(name, required)
    class PathSpec(name: String, required: Boolean, val type: String) : OpenApi2ParameterSpec(name, required)
    class QuerySpec(name: String, required: Boolean, val type: String) : OpenApi2ParameterSpec(name, required)
    class FormSpec(name: String, required: Boolean, val type: String) : OpenApi2ParameterSpec(name, required)
    class BodySpec(name: String, required: Boolean, val schema: SchemaSpec) : OpenApi2ParameterSpec(name, required)

    data class RefSpec(val `$ref`: String) : OpenApi2ParameterSpec(`$ref`, false) {
        val schemaName = `$ref`.cleanSchemaName()
    }

}

data class OpenApi2Spec(val info: InfoSpec,
                        val paths: Map<String, Map<String, OpenApi2PathSpec>>,
                        val definitions: Map<String, SchemaSpec> = emptyMap(),
                        val parameters: Map<String, OpenApi2ParameterSpec> = emptyMap())
