package org.http4k.openapi.v3

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import com.fasterxml.jackson.databind.JsonNode
import org.http4k.openapi.v3.OpenApiJson.asA
import java.math.BigDecimal
import kotlin.reflect.KClass

@JsonTypeInfo(use = NAME, property = "type", defaultImpl = SchemaSpec.RefSpec::class)
@JsonSubTypes(
    Type(value = SchemaSpec.ObjectSpec::class, name = "object"),
    Type(value = SchemaSpec.ArraySpec::class, name = "array"),
    Type(value = SchemaSpec.StringSpec::class, name = "string"),
    Type(value = SchemaSpec.NumberSpec::class, name = "number"),
    Type(value = SchemaSpec.IntegerSpec::class, name = "integer"),
    Type(value = SchemaSpec.BooleanSpec::class, name = "boolean")
)
sealed class SchemaSpec(open val clazz: KClass<*>? = null) {
    data class ObjectSpec(val required: List<String> = emptyList(), val properties: Map<String, SchemaSpec> = emptyMap(), override val clazz: KClass<*>?) : SchemaSpec(clazz)
    data class ArraySpec(private val items: JsonNode) : SchemaSpec() {
        fun itemsSpec(): SchemaSpec =  try {
            items.asA()
        } catch (e: Exception) {
            try {
                items.get("items").asA()
            } catch (e: Exception) {
                ObjectSpec(clazz = Any::class)
            }
        }
    }

    object IntegerSpec : SchemaSpec(Int::class)
    object NumberSpec : SchemaSpec(BigDecimal::class)
    object StringSpec : SchemaSpec(String::class)
    object BooleanSpec : SchemaSpec(Boolean::class)
    data class RefSpec(val `$ref`: String) : SchemaSpec() {
        val schemaName = `$ref`.removePrefix("#/components/schemas/")
    }
}

@JsonTypeInfo(use = NAME, property = "in")
@JsonSubTypes(
    Type(value = ParameterSpec.PathSpec::class, name = "path"),
    Type(value = ParameterSpec.HeaderSpec::class, name = "header"),
    Type(value = ParameterSpec.QuerySpec::class, name = "query"),
    Type(value = ParameterSpec.CookieSpec::class, name = "cookie")
)
sealed class ParameterSpec(val name: String, val required: Boolean, val description: String?, val schema: SchemaSpec) {
    class CookieSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class HeaderSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class PathSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class QuerySpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
}

data class RequestBodySpec(val content: Map<String, MessageBodySpec> = emptyMap())

data class MessageBodySpec(val schema: SchemaSpec?)

data class ComponentsSpec(val schemas: Map<String, SchemaSpec> = emptyMap())

data class ResponseSpec(val content: Map<String, MessageBodySpec>)

data class PathSpec(
    val operationId: String?,
    val summary: String?,
    val description: String?,
    val tags: List<String> = emptyList(),
    val responses: Map<Int, ResponseSpec> = emptyMap(),
    val requestBody: RequestBodySpec?,
    val parameters: List<ParameterSpec> = emptyList()
)

data class InfoSpec(val title: String)
data class OpenApi3Spec(val info: InfoSpec, val paths: Map<String, Map<String, PathSpec>>, val components: ComponentsSpec = ComponentsSpec())
