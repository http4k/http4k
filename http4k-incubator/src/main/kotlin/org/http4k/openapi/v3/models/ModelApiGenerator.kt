package org.http4k.openapi.v3.models

import com.squareup.kotlinpoet.FileSpec
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.GeneratedType
import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.NamedSchema
import org.http4k.openapi.buildModelClass
import org.http4k.openapi.clean
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.flatten
import org.http4k.openapi.v3.flattenedPaths
import org.http4k.poet.buildFormatted
import org.http4k.poet.childClassName

object ModelApiGenerator : ApiGenerator<OpenApi3Spec> {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions): List<FileSpec> = with(spec.flatten()) {
        val componentSchemas = components.schemas.entries
            .fold(mutableMapOf<String, GeneratedType>()) { acc, (name, schema) ->
                schema.buildModelClass(options.packageName("model").childClassName(name), components.schemas, acc)
                acc
            }

        flattenedPaths().forEach { path ->
            (path.requestSchemas() + path.responseSchemas()).forEach {
                if (it is NamedSchema.Generated && it.name.isNotEmpty()) it.schema.buildModelClass(options.packageName("model").childClassName(it.name), components.schemas, componentSchemas)
            }
        }

        componentSchemas.values.distinct().map {
            FileSpec.builder(options.packageName("model"), it.name.clean().capitalize()).apply {
                when (it) {
                    is GeneratedType.GeneratedClass -> addType(it.spec)
                    is GeneratedType.GeneratedTypeAlias -> addTypeAlias(it.spec)
                }
            }.buildFormatted()
        }
    }
}
