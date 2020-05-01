package org.http4k.openapi.server

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OPERATOR
import com.squareup.kotlinpoet.TypeSpec.Companion.objectBuilder
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.OpenApi3Spec
import org.http4k.poet.Property
import org.http4k.routing.RoutingHttpHandler
import java.io.File

data class GenerationOptions(private val basePackage: String, val destinationFolder: File) {
    fun packageName(name: String) = "$basePackage.$name"
}

object ServerApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions) = with(spec) {
        val className = spec.info.title.capitalize() + "Server"

        val endpoints = buildEndpoints()
        val server = objectBuilder(className.capitalize())
            .addFunction(constructionMethod(endpoints))
            .build()

        endpoints.map { it.asFileSpec(options.packageName("endpoints")) } +
            endpoints
                .fold(FileSpec.builder(options.packageName("server"), server.name!!)) { acc, next ->
                    acc.addImport(options.packageName("endpoints"), next.name)
                }
                .addType(server)
                .build()
    }

    private fun OpenApi3Spec.constructionMethod(endpoints: List<FunSpec>) = FunSpec.builder("invoke")
        .addModifiers(OPERATOR)
        .addCode(buildApi(endpoints))
        .build()

    private fun OpenApi3Spec.buildApi(endpoints: List<FunSpec>): CodeBlock {
        val code = endpoints.map { CodeBlock.builder().addStatement(it.name + "()").build() }.joinToString(", ")
        return CodeBlock.builder().addStatement(
            "return org.http4k.routing.routes(\n$code)"
        ).build()
    }

    private fun OpenApi3Spec.buildEndpoints() = paths.flatMap { (path, value) ->
        value.entries.map {
            val functionName = it.value.operationId ?: it.key + path.replace('/', '_')
            FunSpec.builder(functionName.capitalize())
                .returns(Property<RoutingHttpHandler>().type)
                .addCode("TODO()")
                .build()
        }
    }.sortedBy { it.name }
}

private fun FunSpec.asFileSpec(packageName: String) = FileSpec.builder(packageName, name).addFunction(this).build()

//        return buildEndpoints().fold(
//            FunSpec.builder("invoke")
//                .addModifiers(OVERRIDE, OPERATOR)
//                .returns(Property<Response>().type)
//                .addParameter(Property<Request>())
//                .addCode("return Response(org.http4k.core.Status.OK)")
//                .build()) { acc, next ->
//            acc
//        }
