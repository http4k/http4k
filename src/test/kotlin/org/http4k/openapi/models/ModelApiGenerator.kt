package org.http4k.openapi.models

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.OpenApi3Spec
import org.http4k.poet.buildFormatted

object ModelApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions): List<FileSpec> = with(spec) {

        val schemas = components.schemas.entries.fold(mutableMapOf<String, TypeSpec>()) { acc, (name, schema) ->
            acc += (name.capitalize() to acc.getOrDefault(name.capitalize(), schema.buildModelClass(name, components.schemas, acc)))
            acc
        }

        schemas.values.map {
            FileSpec.builder(options.packageName("model"), it.name!!)
                .addType(it)
                .buildFormatted()
        }
    }
}
