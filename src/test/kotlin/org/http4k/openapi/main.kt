package org.http4k.openapi

import org.http4k.openapi.client.ClientApiGenerator
import org.http4k.openapi.models.ModelApiGenerator
import org.http4k.openapi.server.GenerationOptions
import org.http4k.openapi.server.ServerApiGenerator
import java.io.File

fun main(args: Array<String>) {
    val generationOptions = GenerationOptions("org.http4k", File("src/main/kotlin"))

    val targetGeneratedDir = File("src/main/kotlin").apply { mkdirs() }
    val spec = OpenApiJson.asA(File(args[0]).readText(), OpenApi3Spec::class)


    listOf(ModelApiGenerator, ClientApiGenerator, ServerApiGenerator)
        .flatMap { it(spec, generationOptions) }
        .forEach { it.writeTo(targetGeneratedDir) }
}

