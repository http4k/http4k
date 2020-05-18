package org.http4k.openapi

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import java.math.BigDecimal
import kotlin.reflect.KClass
import org.http4k.openapi.OpenApiJson.asA

data class InfoSpec(val title: String)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = SchemaSpec.RefSpec::class)
@JsonSubTypes(
    JsonSubTypes.Type(value = SchemaSpec.ObjectSpec::class, name = "object"),
    JsonSubTypes.Type(value = SchemaSpec.ArraySpec::class, name = "array"),
    JsonSubTypes.Type(value = SchemaSpec.StringSpec::class, name = "string"),
    JsonSubTypes.Type(value = SchemaSpec.NumberSpec::class, name = "number"),
    JsonSubTypes.Type(value = SchemaSpec.IntegerSpec::class, name = "integer"),
    JsonSubTypes.Type(value = SchemaSpec.BooleanSpec::class, name = "boolean")
)
sealed class SchemaSpec(open val clazz: KClass<*>? = null) {

    data class ObjectSpec(val required: List<String> = emptyList(),
                          val properties: Map<String, SchemaSpec> = emptyMap(),
                          override val clazz: KClass<*>?,
                          val additionalProperties: JsonNode? = null) : SchemaSpec(clazz)

    data class ArraySpec(private val items: JsonNode) : SchemaSpec() {
        fun itemsSpec(): SchemaSpec = try {
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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "in")
@JsonSubTypes(
    JsonSubTypes.Type(value = ParameterSpec.PathSpec::class, name = "path"),
    JsonSubTypes.Type(value = ParameterSpec.HeaderSpec::class, name = "header"),
    JsonSubTypes.Type(value = ParameterSpec.QuerySpec::class, name = "query"),
    JsonSubTypes.Type(value = ParameterSpec.FormSpec::class, name = "formData"),
    JsonSubTypes.Type(value = ParameterSpec.BodySpec::class, name = "body"),
    JsonSubTypes.Type(value = ParameterSpec.CookieSpec::class, name = "cookie")
)
sealed class ParameterSpec(val name: String, val required: Boolean, val description: String?, val schema: SchemaSpec) {
    class CookieSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class HeaderSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class PathSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class QuerySpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class FormSpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
    class BodySpec(name: String, required: Boolean, description: String?, schema: SchemaSpec) : ParameterSpec(name, required, description, schema)
}
