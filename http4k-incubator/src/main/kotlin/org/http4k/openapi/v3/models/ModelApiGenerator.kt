package org.http4k.openapi.v3.models

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.v3.ApiGenerator
import org.http4k.openapi.v3.GenerationOptions
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.flattenedPaths
import org.http4k.poet.buildFormatted

object ModelApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions): List<FileSpec> = with(spec) {
        val componentSchemas = components.schemas.entries
            .fold(mutableMapOf<String, TypeSpec>()) { acc, (name, schema) ->
                val nameCapitalized = name.capitalize()
                acc.getOrPut(nameCapitalized, { schema.buildModelClass(ClassName(options.packageName("model"), nameCapitalized), components.schemas, acc) })
                acc
            }

        spec.flattenedPaths().forEach { path ->
            path.allSchemas().forEach { (name, spec) ->
                spec.buildModelClass(ClassName(options.packageName("model"), name), components.schemas, componentSchemas)
            }
        }

        componentSchemas.values.distinct().map {
            FileSpec.builder(options.packageName("model"), it.name!!)
                .addType(it)
                .buildFormatted()
        }
    }
}
