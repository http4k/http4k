package org.http4k.openapi.v3.server

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import org.http4k.openapi.v3.OpenApi3Spec

fun OpenApi3Spec.buildApi(endpoints: List<FunSpec>): CodeBlock {
    val code = endpoints
        .map { CodeBlock.builder().addStatement("\t${it.name}()").build() }
        .joinToString(", ")
    return CodeBlock.builder().addStatement(
        "return %M(\n$code)", MemberName("org.http4k.routing", "routes")
    ).build()
}
