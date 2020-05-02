package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.OpenApiExtension
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson
import kotlin.reflect.full.createInstance

/**
 * Defaults for configuring OpenApi3 with Jackson
 */
fun OpenApi3(apiInfo: ApiInfo, json: Jackson, extensions: List<OpenApiExtension> = emptyList()) = OpenApi3(apiInfo, json, extensions, ApiRenderer.Auto(json, AutoJsonToJsonSchema(json)))

fun AutoJsonToJsonSchema(json: Jackson) = AutoJsonToJsonSchema(json, FieldRetrieval.compose(SimpleLookup(),
    FieldRetrieval.compose(JacksonJsonPropertyAnnotated, JacksonJsonNamingAnnotated(json))))

object JacksonJsonPropertyAnnotated : FieldRetrieval {
    override fun invoke(target: Any, name: String) =
        SimpleLookup()(target, target.javaClass.findName(name) ?: throw NoFieldFound(name, this))

    private fun Class<Any>.findName(name: String): String? = kotlin.constructors.first().parameters
        .mapNotNull { f ->
            f.annotations.filterIsInstance<JsonProperty>().find { it.value == name }
                ?.let { f.name }
        }.firstOrNull() ?: try {
        superclass?.findName(name)
    } catch (e: IllegalStateException) {
        throw NoFieldFound(name, this, e)
    }
}

class JacksonJsonNamingAnnotated(private val json: ConfigurableJackson = Jackson) : FieldRetrieval {
    override fun invoke(target: Any, name: String) = SimpleLookup(renamingStrategyIfRequired(target::class.java))(target, name)

    private fun renamingStrategyIfRequired(clazz: Class<*>): (String) -> String {
        val namingStrategy = clazz.annotations
            .filterIsInstance<JsonNaming>()
            .map { it.value }.getOrNull(0)
            ?.let { it.createInstance() as PropertyNamingStrategy.PropertyNamingStrategyBase }
            ?: json.mapper.propertyNamingStrategy

        return if ( namingStrategy is PropertyNamingStrategy.PropertyNamingStrategyBase) {
            { name: String -> namingStrategy.translate(name) }
        } else {
            { name -> name }
        }
    }
}
