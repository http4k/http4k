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
        val schemaName = `$ref`.removePrefix("#/components/schemas/").removePrefix("#/definitions/")
    }
}

data class ResponseSpec(val content: Map<String, MessageBodySpec>)

data class MessageBodySpec(val schema: SchemaSpec?)
