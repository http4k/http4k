package org.http4k.openapi.v3.server

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OPERATOR
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.v3.OpenApi3Spec

fun OpenApi3Spec.buildServer(endpoints: List<FunSpec>): TypeSpec {
    val className = info.title.capitalize() + "Server"

    return TypeSpec.objectBuilder(className.capitalize())
        .addFunction(constructionMethod(endpoints))
        .build()
}

private fun OpenApi3Spec.constructionMethod(endpoints: List<FunSpec>) = FunSpec.builder("invoke")
    .addModifiers(OPERATOR)
    .addCode(buildApi(endpoints))
    .build()

