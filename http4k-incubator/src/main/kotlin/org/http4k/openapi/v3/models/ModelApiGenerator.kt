package org.http4k.openapi.v3.models

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.NamedSchema
import org.http4k.openapi.buildModelClass
import org.http4k.openapi.clean
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.flattenedPaths
import org.http4k.poet.buildFormatted
import org.http4k.poet.childClassName

object ModelApiGenerator : ApiGenerator<OpenApi3Spec> {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions): List<FileSpec> = with(spec) {
        val componentSchemas = components.schemas.entries
            .fold(mutableMapOf<String, TypeSpec>()) { acc, (name, schema) ->
                schema.buildModelClass(options.packageName("model").childClassName(name), components.schemas, acc)
                acc
            }

        spec.flattenedPaths().forEach { path ->
            path.allSchemas().forEach {
                if (it is NamedSchema.Generated) it.schema.buildModelClass(options.packageName("model").childClassName(it.name), components.schemas, componentSchemas)
            }
        }

        componentSchemas.values.distinct().map {
            FileSpec.builder(options.packageName("model"), it.name!!.clean().capitalize())
                .addType(it)
                .buildFormatted()
        }
    }
}
