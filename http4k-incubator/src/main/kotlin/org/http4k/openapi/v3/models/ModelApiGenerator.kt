package org.http4k.openapi.v3.models

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.v3.ApiGenerator
import org.http4k.openapi.v3.GenerationOptions
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.flattenedPaths
import org.http4k.poet.buildFormatted

object ModelApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions): List<FileSpec> = with(spec) {
        val allSchemas = components.schemas.entries.fold(mutableMapOf<String, TypeSpec>()) { acc, (name, schema) ->
            val nameCapitalized = name.capitalize()
            acc += (nameCapitalized to acc.getOrDefault(nameCapitalized, schema.buildModelClass(nameCapitalized, components.schemas, acc)))
            acc
        }

        spec.flattenedPaths().forEach { path ->
            path.requestSchemas().forEach { (name, spec) ->
                spec.also {
                    allSchemas.getOrPut(name, { it.buildModelClass(name, components.schemas, allSchemas) })
                }
            }
            path.responseSchemas().forEach { (name, spec) ->
                spec.also {
                    allSchemas.getOrPut(name, { it.buildModelClass(name, components.schemas, allSchemas) })
                }
            }
        }

        allSchemas.values.distinct().map {
            FileSpec.builder(options.packageName("model"), it.name!!)
                .addType(it)
                .buildFormatted()
        }
    }
}
