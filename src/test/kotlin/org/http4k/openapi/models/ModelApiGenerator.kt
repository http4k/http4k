package org.http4k.openapi.models

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.OpenApi3Spec

object ModelApiGenerator : ApiGenerator {
    override fun invoke(p1: OpenApi3Spec): List<FileSpec> = with(p1) {
        val schemas = components.schemas.entries.fold(emptyMap<String, TypeSpec>()) { acc, next ->
            acc + (next.key to acc.getOrElse(next.key, {
                TypeSpec.classBuilder(next.key.capitalize()).addModifiers(KModifier.DATA).build()
            }))
        }

        val model = FileSpec.builder("", "model").apply {
            schemas.values.forEach { addType(it) }
        }.build()

        listOf(model)
    }
}
