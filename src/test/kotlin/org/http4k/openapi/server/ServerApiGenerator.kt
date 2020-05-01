package org.http4k.openapi.server

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import org.http4k.openapi.ApiGenerator
import org.http4k.openapi.GenerationOptions
import org.http4k.openapi.OpenApi3Spec

object ServerApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions) = with(spec) {

        val endpoints = buildEndpoints()

        val server = buildServer(endpoints)

        endpoints.map { it.asFileSpec(options.packageName("endpoints")) } +
            endpoints
                .fold(FileSpec.builder(options.packageName("server"), server.name!!)) { acc, next ->
                    acc.addImport(options.packageName("endpoints"), next.name)
                }
                .addType(server)
                .build()
    }
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
