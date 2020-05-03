package org.http4k.openapi.models

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec.Companion.constructorBuilder
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Companion.classBuilder
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

    private fun buildClass(name: String) = classBuilder(name.capitalize())
        .addModifiers(KModifier.DATA)
        .primaryConstructor(constructorBuilder()
            .addParameter("name", String::class)
            .build())
        .addProperty(PropertySpec.builder("name", String::class)
            .initializer("name")
            .build())
        .build()
}
