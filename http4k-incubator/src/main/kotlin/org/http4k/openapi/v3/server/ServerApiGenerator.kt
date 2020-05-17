package org.http4k.openapi.v3.server

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeSpec
import org.http4k.openapi.v3.ApiGenerator
import org.http4k.openapi.v3.GenerationOptions
import org.http4k.openapi.v3.OpenApi3Spec
import org.http4k.openapi.v3.flattenedPaths
import org.http4k.poet.buildFormatted

object ServerApiGenerator : ApiGenerator {
    override fun invoke(spec: OpenApi3Spec, options: GenerationOptions) = with(spec) {
        val endpoints = flattenedPaths().map { it.buildEndpoint(options.packageName("model")) }

        val server = buildServer(endpoints)

        endpoints.map { it.asFileSpec(options.packageName("server.endpoints")) } +
            endpoints
                .fold(FileSpec.builder(options.packageName("server"), server.name!!)) { acc, next ->
                    acc.addImport(options.packageName("server.endpoints"), next.name)
                }
                .addType(server)
                .addFunction(server.buildMain())
                .buildFormatted()
    }
}

private fun TypeSpec.buildMain() = FunSpec.builder("main")
    .addStatement("%N().%M(SunHttp(8000)).start()", this, MemberName("org.http4k.server", "asServer"))
    .build()

private fun FunSpec.asFileSpec(packageName: String) = FileSpec.builder(packageName, name).addFunction(this).indent("\t").build()
