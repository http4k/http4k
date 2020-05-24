package org.http4k.openapi

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import org.http4k.format.Jackson.asA
import java.math.BigDecimal
import kotlin.reflect.KClass

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
                          override val clazz: KClass<*>? = null,
                          val additionalProperties: JsonNode? = null) : SchemaSpec(clazz)

    data class ArraySpec(private val items: JsonNode, val minItems: Int? = null, val maxItems: Int? = null, val uniqueItems: Boolean = false, val nullable: Boolean? = null) : SchemaSpec() {
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

    data class IntegerSpec(val minimum: Number? = null,
                      val maximum: Number? = null,
                      val format: String? = null,
                      val exclusiveMinimum: Number? = null,
                      val exclusiveMaximum: Number? = null,
                      val multipleOf: Number? = null,
                      val nullable: Boolean? = null) : SchemaSpec(Int::class)

    data class NumberSpec(val minimum: Number? = null,
                     val maximum: Number? = null,
                     val format: String? = null,
                     val exclusiveMinimum: Number? = null,
                     val exclusiveMaximum: Number? = null,
                     val multipleOf: Number? = null,
                     val nullable: Boolean? = null) : SchemaSpec(BigDecimal::class)

    data class StringSpec(val minLength: Int? = null, val maxLength: Int? = null, val format: String? = null, val nullable: Boolean? = null) : SchemaSpec(String::class)
    data class BooleanSpec(val nullable: Boolean? = null) : SchemaSpec(Boolean::class)
    data class RefSpec(val `$ref`: String?) : SchemaSpec() {
        val schemaName = `$ref`!!.cleanSchemaName()
    }
}

data class ResponseSpec(val content: Map<String, MessageBodySpec>)

data class MessageBodySpec(val schema: SchemaSpec?)
