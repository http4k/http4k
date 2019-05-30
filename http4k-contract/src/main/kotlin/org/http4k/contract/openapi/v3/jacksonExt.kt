package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.format.Jackson

fun OpenApi3(apiInfo: ApiInfo, json: Jackson) = OpenApi3(
    apiInfo, json,
    ApiRenderer.Auto(json, AutoJsonToJsonSchema(json, FieldRetrieval.compose(SimpleLookup, JacksonAnnotated))))

object JacksonAnnotated : FieldRetrieval {
    override fun invoke(target: Any, name: String): Field {
        val message = target.javaClass.kotlin.constructors.first().parameters
            .mapNotNull { f ->
                f.annotations.filterIsInstance<JsonProperty>().find { it.value == name }
                    ?.let { f.name }
            }.firstOrNull() ?: throw NoFieldFound

        return SimpleLookup(target, message)
    }
}
