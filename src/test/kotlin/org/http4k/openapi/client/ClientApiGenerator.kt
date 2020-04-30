package org.http4k.openapi.client

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.OpenApi3Spec

object ClientApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec) =
        with(spec) {
            val name = info.title.capitalize() + "Client"

            val functions = paths.flatMap {
                val path = it.key
                it.value.entries.map {
                    val functionName = it.value.operationId ?: it.key + path.replace('/', '_')
                    FunSpec.builder(functionName).build()
                }
            }.sortedBy { it.name }

            val classBuilder = TypeSpec.classBuilder(spec.info.title.capitalize())
            val classWithFunctions = functions.fold(classBuilder, TypeSpec.Builder::addFunction)

            listOf(
                FileSpec.builder("", name)
                    .addType(classWithFunctions.build())
                    .build()
            )
        }
}
