package org.http4k.openapi.server

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OPERATOR
import com.squareup.kotlinpoet.TypeSpec.Companion.objectBuilder
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.OpenApi3Spec
import java.io.File

data class GenerationOptions(private val basePackage: String, val destinationFolder: File) {
    fun packageName(name: String) = "$basePackage.$name"
}

object ServerApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions) = with(spec) {
        val className = spec.info.title.capitalize() + "Server"

        val endpoints = buildEndpoints()
        val server = objectBuilder(className.capitalize())
            .addFunction(constructionMethod())
            .build()

        endpoints.map { it.asFileSpec(options.packageName("endpoints")) } +
            FileSpec.builder(options.packageName("server"), server.name!!)
                .addType(server)
                .build()
    }

    private fun OpenApi3Spec.constructionMethod() = FunSpec.builder("invoke")
        .addModifiers(OPERATOR)
        .addCode(buildApi())
        .build()

    private fun OpenApi3Spec.buildApi() = CodeBlock.of("return org.http4k.routing.routes()")
//
//
//
//        return buildEndpoints().fold(
//            FunSpec.builder("invoke")
//                .addModifiers(OVERRIDE, OPERATOR)
//                .returns(Property<Response>().type)
//                .addParameter(Property<Request>())
//                .addCode("return Response(org.http4k.core.Status.OK)")
//                .build()) { acc, next ->
//            acc
//        }

//        return paths.flatMap {
//            FileSpec.builder("", )
//
//            val path = it.key
//            it.value.entries.map {
//                val functionName = it.value.operationId ?: it.key + path.replace('/', '_')
//                FunSpec.builder(functionName).build()
//            }
//        }.sortedBy { it.name }

    private fun OpenApi3Spec.buildEndpoints() = paths.flatMap {
        val path = it.key
        it.value.entries.map {
            val functionName = it.value.operationId ?: it.key + path.replace('/', '_')
            FunSpec.builder(functionName.capitalize()).build()
        }
    }.sortedBy { it.name }

}

private fun FunSpec.asFileSpec(packageName: String) = FileSpec.builder(packageName, name).addFunction(this).build()
