package org.http4k.openapi.models

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.OpenApi3Spec
import org.http4k.poet.buildFormatted

object ModelApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions): List<FileSpec> = with(spec) {

        val schemas = components.schemas.entries.fold(emptyMap<String, TypeSpec>()) { acc, next ->
            acc + (next.key.capitalize() to acc.getOrDefault(next.key.capitalize(), buildClass(next.key)))
        }

        schemas.values.map {
            FileSpec.builder(options.packageName("model"), it.name!!)
                .addType(it)
                .buildFormatted()
        }
    }
}
