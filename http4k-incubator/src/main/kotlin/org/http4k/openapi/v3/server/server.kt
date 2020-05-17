package org.http4k.openapi.v3.server

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OPERATOR
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.apiName

fun OpenApi3Spec.buildServer(endpoints: List<FunSpec>) =
    TypeSpec.objectBuilder((apiName() + "Server").capitalize())
        .addFunction(constructionMethod(endpoints))
        .build()

private fun OpenApi3Spec.constructionMethod(endpoints: List<FunSpec>) = FunSpec.builder("invoke")
    .addModifiers(OPERATOR)
    .addCode(buildApi(endpoints))
    .build()

