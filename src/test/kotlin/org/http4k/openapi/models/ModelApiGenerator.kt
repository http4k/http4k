package org.http4k.openapi.models

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec.Companion.constructorBuilder
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeSpec.Companion.classBuilder
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.OpenApi3Spec
import org.http4k.openapi.SchemaSpec
import org.http4k.openapi.server.GenerationOptions

object ModelApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions): List<FileSpec> = with(spec) {

        val schemas = components.schemas.entries.fold(emptyMap<String, TypeSpec>()) { acc, next ->
            acc + (next.key.capitalize() to acc.getOrDefault(next.key.capitalize(), buildClass(next)))
        }

        val model = FileSpec.builder(options.packageName("model"), "model").apply {
            schemas.values.forEach { addType(it) }
        }.build()

        listOf(model)
    }

    private fun buildClass(next: Map.Entry<String, SchemaSpec>) = classBuilder(next.key.capitalize())
        .addModifiers(KModifier.DATA)
        .primaryConstructor(constructorBuilder()
            .addParameter("name", String::class)
            .build())
        .addProperty(PropertySpec.builder("name", String::class)
            .initializer("name")
            .build())
        .build()
}
