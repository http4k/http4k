package org.http4k.contract.jsonschema.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance


object JacksonJsonPropertyAnnotated : FieldRetrieval {
    override fun invoke(target: Any, name: String) =
        SimpleLookup(
            metadataRetrievalStrategy = JacksonFieldMetadataRetrievalStrategy
        )(target, target.javaClass.findName(name) ?: throw NoFieldFound(name, this))

    private fun Class<Any>.findName(name: String): String? =
        kotlin.constructors.first().parameters.firstNotNullOfOrNull { f ->
            f.annotations.filterIsInstance<JsonProperty>().find { it.value == name }
                ?.let { f.name }
        }
            ?: try {
                superclass?.findName(name)
            } catch (e: IllegalStateException) {
                throw NoFieldFound(name, this, e)
            }
}

class JacksonJsonNamingAnnotated(private val json: ConfigurableJackson = Jackson) : FieldRetrieval {
    override fun invoke(target: Any, name: String) = SimpleLookup(
        renamingStrategyIfRequired(target::class.java),
        JacksonFieldMetadataRetrievalStrategy
    )(target, name)

    private fun renamingStrategyIfRequired(clazz: Class<*>): (String) -> String {
        val namingStrategy = clazz.annotations
            .filterIsInstance<JsonNaming>()
            .map { it.value }.getOrNull(0)
            ?.let { it.createInstance() as PropertyNamingStrategy }
            ?: json.mapper.propertyNamingStrategy

        return if (namingStrategy is PropertyNamingStrategies.NamingBase) {
            { name: String -> namingStrategy.translate(name) }
        } else {
            { name -> name }
        }
    }
}

object JacksonFieldMetadataRetrievalStrategy : FieldMetadataRetrievalStrategy {
    override fun invoke(target: Any, fieldName: String): FieldMetadata =
        FieldMetadata(target.javaClass.findPropertyDescription(fieldName)?.let { mapOf("description" to it) }
            .orEmpty())

    /**
     * Scan all constructors until one contains the property named [name] and has an [JsonPropertyDescription]
     * annotation with non-null value.
     *
     * By scanning multiple constructors, this also works in cases with generated constructors and no-arg constructors.
     */
    private fun Class<Any>.findPropertyDescription(name: String): String? =
        kotlin.constructors.asSequence()
            .mapNotNull { constructor ->
                constructor.parameters.find { parameter ->
                    parameter.kind == KParameter.Kind.VALUE && parameter.name == name
                }
            }
            .map { parameter -> parameter.annotations.filterIsInstance<JsonPropertyDescription>().firstOrNull() }
            .firstNotNullOfOrNull { annotation -> annotation?.value } ?: superclass?.findPropertyDescription(name)
}
