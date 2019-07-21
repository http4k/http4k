package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.v3.JacksonJsonPropertyAnnotated.findName
import org.http4k.format.Jackson
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

/**
 * Sensible default objects for using Jackson with minimal fuss.
 */

fun OpenApi3(apiInfo: ApiInfo, json: Jackson) = OpenApi3(apiInfo, json, ApiRenderer.Auto(json, AutoJsonToJsonSchema(json)))

fun AutoJsonToJsonSchema(json: Jackson) = AutoJsonToJsonSchema(json, FieldRetrieval.compose(SimpleLookup, JacksonAnnotated))

val JacksonAnnotated = FieldRetrieval.compose(JacksonJsonPropertyAnnotated, JacksonJsonNamingAnnotated)

object JacksonJsonPropertyAnnotated : FieldRetrieval {
    override fun invoke(target: Any, name: String) =
            SimpleLookup(target, target.javaClass.findName(name) ?: throw NoFieldFound(name, this))

    private fun Class<Any>.findName(name: String): String? = kotlin.constructors.first().parameters
            .mapNotNull { f ->
                f.annotations.filterIsInstance<JsonProperty>().find { it.value == name }
                        ?.let { f.name }
            }.firstOrNull() ?: try {
        superclass.findName(name)
    } catch (e: IllegalStateException) {
        throw NoFieldFound(name, this, e)
    }
}

object JacksonJsonNamingAnnotated : FieldRetrieval {

    override fun invoke(target: Any, name: String): Field {
        val renmaingStrategy = renamingStrategyIfRequired(target::class.java)
        val fields = try {
            target::class.memberProperties.map { renmaingStrategy(it.name) to it }.toMap()
        } catch (e: Error) {
            emptyMap<String, KProperty1<out Any, Any?>>()
        }

        return fields[name]
                ?.let { field ->
                    field.javaGetter
                            ?.let { it(target) }
                            ?.let { it to field.returnType.isMarkedNullable }
                }
                ?.let { Field(it.first, it.second) } ?: throw NoFieldFound(name, target)

    }

    private fun renamingStrategyIfRequired(clazz: Class<*>): (String) -> String {
        val namingStrategyClazz = clazz.annotations
                .filterIsInstance<JsonNaming>()
                .map { it.value }
                .getOrElse(0) { PropertyNamingStrategy::class }
        if (namingStrategyClazz.isSubclassOf(PropertyNamingStrategy.PropertyNamingStrategyBase::class)) {
            val namingStrategy = namingStrategyClazz.createInstance() as PropertyNamingStrategy.PropertyNamingStrategyBase
            return { name: String -> namingStrategy.translate(name) }
        } else {
            return { name -> name }
        }
    }

}
