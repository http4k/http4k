package org.http4k.openapi.server

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import org.http4k.openapi.OpenApi3Spec

fun OpenApi3Spec.buildApi(endpoints: List<FunSpec>): CodeBlock {
    val code = endpoints
        .map { CodeBlock.builder().addStatement(it.name + "()").build() }
        .joinToString(", ")
    return CodeBlock.builder().addStatement(
        "return org.http4k.routing.routes(\n$code)"
    ).build()
}
